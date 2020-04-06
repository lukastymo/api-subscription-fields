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

package uk.gov.hmrc.apisubscriptionfields.service

import java.util.UUID

import cats.data.NonEmptyList
import javax.inject._
import uk.gov.hmrc.apisubscriptionfields.model._
import uk.gov.hmrc.apisubscriptionfields.repository.{SubscriptionFields, SubscriptionFieldsRepository}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class UUIDCreator {
  def uuid(): UUID = UUID.randomUUID()
}

@Singleton
class SubscriptionFieldsService @Inject()(repository: SubscriptionFieldsRepository,
                                          uuidCreator: UUIDCreator,
                                          fieldsDefinitionService: FieldsDefinitionService) {

  def validate(id: ClientId, context: ApiContext, version: ApiVersion, fields: Fields): SubsFieldValiationResponse = {
    val fieldNames = fields.keys
    val fieldDefinitionResponse: Future[Option[FieldsDefinitionResponse]] = fieldsDefinitionService.get(context, version)
    val fieldDefinitions: Future[Option[Seq[FieldDefinition]]] = fieldDefinitionResponse.map(_.map(_.fieldDefinitions))


    fieldDefinitions.map(
      _.map
        {
          unpackedFieldDefinitions => unpackedFieldDefinitions.map {
            unpackedFieldDefinition =>
            val regexs: Seq[Option[String]] = getRegexs(unpackedFieldDefinition)
            fields.map { case (key, value) => (key, matchAllRegexs(regexs, value)) }.filterKeys(_ == false) match {
              case Map.empty => ValidSubsFieldValiationResponse
              case mapOfFieldsWithErrors => {
                val response = InvalidSubsFieldValiationResponse(Set.empty)
                mapOfFieldsWithErrors.keys
                  .map(subsFieldName => fieldDefinitions
                    .map(_.map(_.map(_.validation
                      .map(x => {
                        response.errorResponses += InvalidSubsFieldResponse(subsFieldName, x.errorMessage)
                      }
                      )))))
              }
            }
          }
        }
    )
  }


  def upsert(clientId: ClientId, apiContext: ApiContext, apiVersion: ApiVersion, subscriptionFields: Fields): Future[(SubscriptionFieldsResponse, IsInsert)] = {
    val fields = SubscriptionFields(clientId.value, apiContext.value, apiVersion.value, uuidCreator.uuid(), subscriptionFields)
    repository.saveAtomic(fields).map(tuple => (asResponse(tuple._1), tuple._2))
  }

  def delete(clientId: ClientId, apiContext: ApiContext, apiVersion: ApiVersion): Future[Boolean] = {
    repository.delete(clientId, apiContext, apiVersion)
  }

  def delete(clientId: ClientId): Future[Boolean] = {
    repository.delete(clientId)
  }

  def get(clientId: ClientId, apiContext: ApiContext, apiVersion: ApiVersion): Future[Option[SubscriptionFieldsResponse]] = {
    for {
      fetch <- repository.fetch(clientId, apiContext, apiVersion)
    } yield fetch.map(asResponse)
  }

  def get(subscriptionFieldsId: SubscriptionFieldsId): Future[Option[SubscriptionFieldsResponse]] = {
    for {
      fetch <- repository.fetchByFieldsId(subscriptionFieldsId)
    } yield fetch.map(asResponse)
  }

  def get(clientId: ClientId): Future[Option[BulkSubscriptionFieldsResponse]] = {
    (for {
      fields <- repository.fetchByClientId(clientId)
    } yield fields.map(asResponse)) map {
      case Nil => None
      case fs => Some(BulkSubscriptionFieldsResponse(subscriptions = fs))
    }
  }

  def getAll: Future[BulkSubscriptionFieldsResponse] = {
    (for {
      fields <- repository.fetchAll()
    } yield fields.map(asResponse)) map (BulkSubscriptionFieldsResponse(_))
  }

  private def asResponse(apiSubscription: SubscriptionFields): SubscriptionFieldsResponse = {
    SubscriptionFieldsResponse(
      clientId = apiSubscription.clientId,
      apiContext = apiSubscription.apiContext,
      apiVersion = apiSubscription.apiVersion,
      fieldsId = SubscriptionFieldsId(apiSubscription.fieldsId),
      fields = apiSubscription.fields)
  }

  private def getRegexs(fieldDefinition: FieldDefinition) = {

    fieldDefinition.validation.map((validation: Validation) => validation.rules.asInstanceOf[NonEmptyList[RegexValidationRule]]
        .map((rule: RegexValidationRule) => Seq[String]() += rule.regex)
      )
  }


  private def matchAllRegexs(regexs: Seq[Option[String]], value: String): Boolean = {
    var matches = false
   regexs.foreach(regex => {
     matches = value.matches(regex.get)
   })
    matches
  }


}
