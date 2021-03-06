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

import assets.MessageLookup.{OutsideTaxYears => messages}
import config.AppConfig
import connectors.CalculatorConnector
import controllers.GainController
import controllers.helpers.FakeRequestHelper
import models.resident.{DisposalDateModel, TaxYearModel}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test.Helpers._
import uk.gov.hmrc.play.test.UnitSpec

class OutsideTaxYearsActionSpec extends UnitSpec with GuiceOneAppPerSuite with FakeRequestHelper with MockitoSugar{

  def setupTarget(disposalDateModel: Option[DisposalDateModel], taxYearModel: Option[TaxYearModel]): GainController = {

    val mockCalcConnector = mock[CalculatorConnector]

    when(mockCalcConnector.fetchAndGetFormData[DisposalDateModel](ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(disposalDateModel)

    when(mockCalcConnector.getTaxYear(ArgumentMatchers.any())(ArgumentMatchers.any()))
      .thenReturn(taxYearModel)

    new GainController {
      val calcConnector = mockCalcConnector
      val config: AppConfig = mock[AppConfig]
    }
  }

  "Calling .outsideTaxYears from the GainCalculationController" when {

    "there is a valid session" should {
      lazy val target = setupTarget(Some(DisposalDateModel(10, 10, 2018)), Some(TaxYearModel("2018/19", false, "2018/19")))
      lazy val result = target.outsideTaxYears(fakeRequestWithSession)

      "return a 200" in {
        status(result) shouldBe 200
      }

      "return some html" in {
        contentType(result) shouldBe Some("text/html")
      }

      s"return a title of ${messages.title}" in {
        Jsoup.parse(bodyOf(result)).title shouldBe messages.title
      }

      s"have a back link to '${controllers.routes.GainController.disposalDate().url}'" in {
        Jsoup.parse(bodyOf(result)).getElementById("back-link").attr("href") shouldBe controllers.routes.GainController.disposalDate().url
      }
    }

    "there is no valid session" should {
      lazy val result = GainController.outsideTaxYears(fakeRequest)

      "return a 303" in {
        status(result) shouldBe 303
      }

      "return you to the session timeout page" in {
        redirectLocation(result).get should include ("/calculate-your-capital-gains/resident/properties/session-timeout")
      }
    }
  }
}
