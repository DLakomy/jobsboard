package dlakomy.jobsboard.core

import cats.*
import cats.effect.*
import cats.implicits.*
import dlakomy.jobsboard.domain.job.*
import dlakomy.jobsboard.domain.pagination.*
import dlakomy.jobsboard.logging.syntax.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import doobie.util.*
import org.typelevel.log4cats.Logger

import java.util.UUID


trait Jobs[F[_]]:
  def create(ownerEmail: String, jobInfo: JobInfo): F[UUID]
  def all(): fs2.Stream[F, Job]
  def all(filter: JobFilter, pagination: Pagination): F[List[Job]]
  def find(id: UUID): F[Option[Job]]
  def update(id: UUID, jobInfo: JobInfo): F[Option[Job]]
  def activate(id: UUID): F[Int]
  def delete(id: UUID): F[Int] // Int - number of rows affected
  def possibleFilters(): F[JobFilter]


class LiveJobs[F[_]: MonadCancelThrow: Logger] private (xa: Transactor[F]) extends Jobs[F]:
  override def create(ownerEmail: String, jobInfo: JobInfo): F[UUID] =
    sql"""
      INSERT INTO jobs(
        date
      , ownerEmail
      , company
      , title
      , description
      , externalUrl
      , remote
      , location
      , salaryLo
      , salaryHi
      , currency
      , country
      , tags
      , image
      , seniority
      , other
      , active
      ) VALUES (
        ${System.currentTimeMillis()}
      , ${ownerEmail}
      , ${jobInfo.company}
      , ${jobInfo.title}
      , ${jobInfo.description}
      , ${jobInfo.externalUrl}
      , ${jobInfo.remote}
      , ${jobInfo.location}
      , ${jobInfo.salaryLo}
      , ${jobInfo.salaryHi}
      , ${jobInfo.currency}
      , ${jobInfo.country}
      , ${jobInfo.tags}
      , ${jobInfo.image}
      , ${jobInfo.seniority}
      , ${jobInfo.other}
      , false
      )
    """.update
      .withUniqueGeneratedKeys[UUID]("id")
      .transact(xa)

  override def all(): fs2.Stream[F, Job] =
    sql"""
      SELECT id
           , date
           , ownerEmail
           , company
           , title
           , description
           , externalUrl
           , remote
           , location
           , salaryLo
           , salaryHi
           , currency
           , country
           , tags
           , image
           , seniority
           , other
           , active
        FROM jobs
       WHERE active = true
    """
      .query[Job]
      .stream
      .transact(xa)

  override def all(filter: JobFilter, pagination: Pagination): F[List[Job]] =
    val selectFr =
      fr"""
        SELECT id
             , date
             , ownerEmail
             , company
             , title
             , description
             , externalUrl
             , remote
             , location
             , salaryLo
             , salaryHi
             , currency
             , country
             , tags
             , image
             , seniority
             , other
             , active
      """

    val fromFr = fr"FROM jobs"

    val whereFr = Fragments.whereAndOpt(
      filter.companies.toNel.map(companies => Fragments.in(fr"company", companies)),
      filter.locations.toNel.map(locations => Fragments.in(fr"location", locations)),
      filter.countries.toNel.map(countries => Fragments.in(fr"country", countries)),
      filter.seniorities.toNel.map(seniorities => Fragments.in(fr"seniority", seniorities)),
      filter.tags.toNel.map: tags => // intersection between filter.tags and row's tags
        Fragments.or(tags.map(tag => fr"$tag=any(tags)")),
      filter.maxSalary.map(salary => fr"salaryHi > $salary"),
      // true - only remotes, false - no preference.
      filter.remoteOnly.some.filter(identity).map(remote => fr"remote = $remote"),
      fr"active = true".some
    )

    val orderFragment = fr"ORDER BY date DESC"
    val paginationFr =
      fr"LIMIT ${pagination.limit} OFFSET ${pagination.offset}"

    val statement = selectFr |+| fromFr |+| whereFr |+| orderFragment |+| paginationFr

    statement
      .query[Job]
      .to[List]
      .transact(xa)
      .logError(e => s"Failed query: ${e.getMessage}")

  override def find(id: UUID): F[Option[Job]] =
    sql"""
      SELECT id
           , date
           , ownerEmail
           , company
           , title
           , description
           , externalUrl
           , remote
           , location
           , salaryLo
           , salaryHi
           , currency
           , country
           , tags
           , image
           , seniority
           , other
           , active
        FROM jobs
       WHERE id = $id
         AND active = true
    """
      .query[Job]
      .option
      .transact(xa)

  override def update(id: UUID, jobInfo: JobInfo): F[Option[Job]] =
    sql"""
      UPDATE jobs
         SET company = ${jobInfo.company}
           , title = ${jobInfo.title}
           , description = ${jobInfo.description}
           , externalUrl = ${jobInfo.externalUrl}
           , remote = ${jobInfo.remote}
           , location = ${jobInfo.location}
           , salaryLo = ${jobInfo.salaryLo}
           , salaryHi = ${jobInfo.salaryHi}
           , currency = ${jobInfo.currency}
           , country = ${jobInfo.country}
           , tags = ${jobInfo.tags}
           , image = ${jobInfo.image}
           , seniority = ${jobInfo.seniority}
           , other = ${jobInfo.other}
       WHERE id = $id
    """.update.run
      .transact(xa)
      .flatMap(_ => find(id))

  override def activate(id: UUID): F[Int] =
    sql"""
      UPDATE jobs
         SET active = true
       WHERE id = $id
    """.update.run
      .transact(xa)

  override def delete(id: UUID): F[Int] =
    sql"""
      DELETE FROM jobs
       WHERE id = $id
    """.update.run
      .transact(xa)

  override def possibleFilters(): F[JobFilter] =
    sql"""
      WITH active_jobs as (
        SELECT *
          FROM jobs
        WHERE active = true
      )
      SELECT ARRAY((SELECT DISTINCT company FROM active_jobs)) companies
           , ARRAY((SELECT DISTINCT location FROM active_jobs)) locations
           , ARRAY((SELECT DISTINCT country FROM active_jobs WHERE country IS NOT NULL)) countries
           , ARRAY((SELECT DISTINCT seniority FROM active_jobs WHERE seniority IS NOT NULL)) seniorities
           , ARRAY((SELECT DISTINCT UNNEST(tags) FROM active_jobs)) tags
           , MAX(salaryHi) maxSalaryHi
           , false remote
        FROM active_jobs
    """
      .query[JobFilter]
      .option
      .transact(xa)
      .map(_.getOrElse(JobFilter()))


object LiveJobs:

  def apply[F[_]: MonadCancelThrow: Logger](xa: Transactor[F]): F[LiveJobs[F]] = new LiveJobs[F](xa).pure[F]
