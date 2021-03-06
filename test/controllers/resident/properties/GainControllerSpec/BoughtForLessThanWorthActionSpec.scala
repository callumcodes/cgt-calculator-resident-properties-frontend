/*
 * Copyright 2017 HM Revenue & Customs
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

package controllers.GainControllerSpec

import assets.MessageLookup.{BoughtForLessThanWorth => messages}
import common.KeystoreKeys.{ResidentPropertyKeys => keyStoreKeys}
import config.{AppConfig, ApplicationConfig}
import connectors.CalculatorConnector
import controllers.GainController
import controllers.helpers.FakeRequestHelper
import models.resident.properties.BoughtForLessThanWorthModel
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class BoughtForLessThanWorthActionSpec extends UnitSpec with GuiceOneAppPerSuite with FakeRequestHelper with MockitoSugar {

  def setupTarget(getData: Option[BoughtForLessThanWorthModel]): GainController= {

    val mockCalcConnector = mock[CalculatorConnector]

    when(mockCalcConnector.fetchAndGetFormData[BoughtForLessThanWorthModel](ArgumentMatchers.eq(keyStoreKeys.boughtForLessThanWorth))
      (ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(getData))

    when(mockCalcConnector.saveFormData[BoughtForLessThanWorthModel](ArgumentMatchers.any(), ArgumentMatchers.any())
      (ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(mock[CacheMap]))

    new GainController {
      override val calcConnector: CalculatorConnector = mockCalcConnector
      override val config: AppConfig = ApplicationConfig
    }
  }

  "Calling .boughtForLessThanWorth from the resident GainController" when {

    "request has a valid session and no keystore value" should {

      lazy val target = setupTarget(None)
      lazy val result = target.boughtForLessThanWorth(fakeRequestWithSession)

      "return a status of 200" in {
        status(result) shouldBe 200
      }

      s"return some html with title of ${messages.title}" in {
        contentType(result) shouldBe Some("text/html")
        Jsoup.parse(bodyOf(result)).title shouldEqual messages.title
      }
    }

    "request has a valid session and some keystore value" should {

      lazy val target = setupTarget(Some(BoughtForLessThanWorthModel(true)))
      lazy val result = target.boughtForLessThanWorth(fakeRequestWithSession)

      "return a status of 200" in {
        status(result) shouldBe 200
      }

    }

    "request has an invalid session" should {

      lazy val target = setupTarget(None)
      lazy val result = target.boughtForLessThanWorth(fakeRequest)

      "return a status of 303" in {
        status(result) shouldBe 303
      }

      "return you to the session timeout page" in {
        redirectLocation(result).get should include ("/calculate-your-capital-gains/resident/properties/session-timeout")
      }
    }
  }

  "Calling .submitBoughtForLessThanWorth from the resident GainCalculator" when {

    "a valid form with the answer 'Yes' is submitted" should {

      lazy val target = setupTarget(None)
      lazy val request = fakeRequestToPOSTWithSession(("boughtForLessThanWorth", "Yes"))
      lazy val result = target.submitBoughtForLessThanWorth(request)

      "return a status of 303" in {
        status(result) shouldBe 303
      }

      "redirect to the worth when sold page" in {
        redirectLocation(result) shouldBe Some("/calculate-your-capital-gains/resident/properties/worth-when-bought-for-less")
      }
    }

    "a valid form with the answer 'No' is submitted" should {

      lazy val target = setupTarget(None)
      lazy val request = fakeRequestToPOSTWithSession(("boughtForLessThanWorth", "No"))
      lazy val result = target.submitBoughtForLessThanWorth(request)

      "return a status of 303" in {
        status(result) shouldBe 303
      }

      "redirect to the acquisition value page" in {
        redirectLocation(result) shouldBe Some("/calculate-your-capital-gains/resident/properties/acquisition-value")
      }
    }

    "an invalid form with the answer '' is submitted" should {

      lazy val target = setupTarget(None)
      lazy val request = fakeRequestToPOSTWithSession(("boughtForLessThanWorth", ""))
      lazy val result = target.submitBoughtForLessThanWorth(request)
      lazy val doc = Jsoup.parse(bodyOf(result))

      "return a status of 400" in {
        status(result) shouldBe 400
      }

      "render the Bought for Less than worth page" in {
        doc.title() shouldEqual messages.title
      }
    }
  }
}
