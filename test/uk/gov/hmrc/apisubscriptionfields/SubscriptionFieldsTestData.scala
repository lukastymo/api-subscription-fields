/*
 * Copyright 2020 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.apisubscriptionfields

import java.util.UUID

import uk.gov.hmrc.apisubscriptionfields.model._
import Types._
import scala.concurrent.Future

trait SubscriptionFieldsTestData extends FieldsDefinitionTestData with ValidationRuleTestData {
  import eu.timepit.refined.auto._

  final val FakeRawFieldsId = UUID.randomUUID()
  final val FakeFieldsId = SubscriptionFieldsId(FakeRawFieldsId)

  final val EmptyResponse: Future[Option[SubscriptionFieldsResponse]] = Future.successful(None)
  final val FakeSubscriptionFields: Map[FieldName, String] = Map(fieldN(1) -> "X", fieldN(2) -> "Y")
  final val SubscriptionFieldsMatchRegexValidation: Fields = Map(AlphanumericFieldName -> "ABC123abc", PasswordFieldName -> "Qw12@erty")
  final val SubscriptionFieldsDoNotMatchRegexValidation: Fields = Map(AlphanumericFieldName -> "ABC123abc=", PasswordFieldName -> "Qw12erty")

  final val FakeApiSubscription = SubscriptionFields(fakeRawClientId, fakeRawContext, fakeRawVersion, FakeRawFieldsId, FakeSubscriptionFields)
  final val FakeSubscriptionFieldsId = SubscriptionFieldsId(FakeRawFieldsId)
  final val FakeSubscriptionFieldsResponse: SubscriptionFieldsResponse =
    SubscriptionFieldsResponse(fakeRawClientId, fakeRawContext, fakeRawVersion, FakeSubscriptionFieldsId, FakeSubscriptionFields)
  final val FakeValidSubsFieldValidationResponse: SubsFieldValidationResponse = ValidSubsFieldValidationResponse

  final val CallbackUrlFieldName: FieldName = "callbackUrl"
  final val FakeFieldErrorMessage1: FieldError = ((CallbackUrlFieldName, "Invalid Callback URL"))

  final val EoriFieldName: FieldName = "EORI"
  final val FakeFieldErrorMessage2 = ((EoriFieldName, "Invalid EORI"))
  final val FakeFieldErrorMessages = Map(
    (CallbackUrlFieldName -> FakeFieldErrorMessage1._2),
    (EoriFieldName -> FakeFieldErrorMessage2._2)
  )
  final val FakeInvalidSubsFieldValidationResponse: SubsFieldValidationResponse = InvalidSubsFieldValidationResponse(FakeFieldErrorMessages)

  final val FakeFieldErrorForAlphanumeric: FieldError = ((AlphanumericFieldName, "Needs to be alpha numeric"))
  final val FakeFieldErrorForPassword = ((PasswordFieldName, "Needs to be at least 8 chars with at least one lowercase, uppercase and special char"))
  final val FakeInvalidSubsFieldValidationResponse2 = InvalidSubsFieldValidationResponse(errorResponses = Map(
    (AlphanumericFieldName -> FakeFieldErrorForAlphanumeric._2),
    (PasswordFieldName -> FakeFieldErrorForPassword._2)
  ))

  def createSubscriptionFieldsWithApiContext(clientId: String = fakeRawClientId, rawContext: String = fakeRawContext) = {
    val subscriptionFields: Fields = Map(fieldN(1) -> "value_1", fieldN(2) -> "value_2", fieldN(3) -> "value_3")
    SubscriptionFields(clientId, rawContext, fakeRawVersion, UUID.randomUUID(), subscriptionFields)
  }

  def uniqueClientId = UUID.randomUUID().toString
}

object SubscriptionFieldsTestData extends SubscriptionFieldsTestData

