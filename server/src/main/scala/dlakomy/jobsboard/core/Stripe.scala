package dlakomy.jobsboard.core

import cats.*
import cats.implicits.*
import com.stripe.model.checkout.Session
import com.stripe.param.checkout.SessionCreateParams
import com.stripe.{Stripe => TheStripe}
import dlakomy.jobsboard.config.StripeConfig
import dlakomy.jobsboard.logging.syntax.*
import org.typelevel.log4cats.Logger


trait Stripe[F[_]]:
  def createCheckoutSession(jobId: String, userEmail: String): F[Option[Session]]


class LiveStripe[F[_]: MonadThrow: Logger] private (key: String, price: String, successUrl: String, cancelUrl: String)
    extends Stripe[F]:

  // globally set constant :O this Java is really ugly...
  TheStripe.apiKey = key

  override def createCheckoutSession(jobId: String, userEmail: String): F[Option[Session]] =
    SessionCreateParams
      .builder()
      .setMode(SessionCreateParams.Mode.PAYMENT)
      .setInvoiceCreation(
        // one does not code shortly in Java...
        SessionCreateParams.InvoiceCreation.builder().setEnabled(true).build()
      )
      .setPaymentIntentData(
        SessionCreateParams.PaymentIntentData.builder().setReceiptEmail(userEmail).build()
      )
      .setSuccessUrl(s"$successUrl/$jobId")
      .setCancelUrl(cancelUrl)
      .setCustomerEmail(userEmail)
      .setClientReferenceId(jobId)
      .addLineItem(
        SessionCreateParams.LineItem
          .builder()
          .setQuantity(1L)
          .setPrice(price)
          .build()
      )
      .build()
      .pure[F]
      .map(params => Session.create(params))
      .map(_.some)
      .logError(error => s"Creating checkout session failed: $error")
      .recover:
        case _ => None


object LiveStripe:
  def apply[F[_]: MonadThrow: Logger](stripeConfig: StripeConfig): F[LiveStripe[F]] =
    import stripeConfig.*
    new LiveStripe[F](key, price, successUrl, cancelUrl).pure[F]
