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

package views.resident

import assets.MessageLookup.{LossesBroughtForward => messages, Resident => commonMessages}
import controllers.helpers.FakeRequestHelper
import forms.resident.LossesBroughtForwardForm._
import models.resident.TaxYearModel
import org.jsoup.Jsoup
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import views.html.calculation.{resident => views}

class LossesBroughtForwardViewSpec extends UnitSpec with WithFakeApplication with FakeRequestHelper {

  lazy val postAction = controllers.routes.DeductionsController.submitLossesBroughtForward()

  "Reliefs view" should {

    lazy val view = views.lossesBroughtForward (
      lossesBroughtForwardForm, postAction, "", TaxYearModel("2015/16", true, "2015/16"), "home-link", "navTitle")(fakeRequest, applicationMessages)
    lazy val doc = Jsoup.parse(view.body)

    "have a charset of UTF-8" in {
      doc.charset().toString shouldBe "UTF-8"
    }

    "have a dynamic navTitle of navTitle" in {
      doc.select("span.header__menu__proposition-name").text() shouldBe "navTitle"
    }

    s"have a title ${messages.title("2015/16")}" in {
      doc.title() shouldBe messages.title("2015/16")
    }

    "have a home link to '/calculate-your-capital-gains/resident/properties/'" in {
      doc.getElementById("homeNavHref").attr("href") shouldEqual "/calculate-your-capital-gains/resident/properties/"
    }

    "have a hidden legend" in {
      val legend = doc.select("legend")
      legend.hasClass("visuallyhidden") shouldBe true
    }

    s"have a back link with text ${commonMessages.back}" in {
      doc.select("#back-link").text shouldEqual commonMessages.back
    }

    s"have the question of the page ${messages.question("2015/16")}" in {
      doc.select("h1").text() shouldEqual messages.question("2015/16")
    }

    s"render a form tag with a POST action" in {
      doc.select("form").attr("method") shouldEqual "POST"
    }

    s"have a visually hidden legend for an input with text ${messages.question("2015/16")}" in {
      doc.select("legend.visuallyhidden").text() shouldEqual messages.question("2015/16")
    }

    "have hint text" which {
      lazy val hintText = doc.select("form span")

      "with the class 'form-hint'" in {
        hintText.attr("class") shouldBe "form-hint"
      }

      s"with the message ${messages.helpText}" in {
        hintText.text() shouldBe messages.helpText
      }
    }

    s"have an input field with id option-yes " in {
      doc.body.getElementById("option-yes").tagName() shouldEqual "input"
    }

    s"have an input field with id option-no " in {
      doc.body.getElementById("option-no").tagName() shouldEqual "input"
    }

    "have a continue button " in {
      doc.body.getElementById("continue-button").text shouldEqual commonMessages.continue
    }
  }

  "Losses Brought Forward view with pre-selected value of yes" should {
    lazy val form = lossesBroughtForwardForm.bind(Map(("option", "Yes")))
    lazy val view = views.lossesBroughtForward(form, postAction, "", TaxYearModel("2015/16", true, "2015/16"), "home", "navTitle")(fakeRequest, applicationMessages)
    lazy val doc = Jsoup.parse(view.body)

    "have the option 'Yes' auto selected" in {
      doc.body.getElementById("option-yes").parent.className should include("selected")
    }

    "not have a drop down button" in {
      doc.body.select("summary").isEmpty shouldBe true
    }
  }

  "Losses Brought Forward view with pre-selected value of no" should {
    lazy val form = lossesBroughtForwardForm.bind(Map(("option", "No")))
    lazy val view = views.lossesBroughtForward(form, postAction, "", TaxYearModel("2015/16", true, "2015/16"), "home", "navTitle")(fakeRequest, applicationMessages)
    lazy val doc = Jsoup.parse(view.body)

    "have the option 'No' auto selected" in {
      doc.body.getElementById("option-no").parent.className should include("selected")
    }
  }

  "Losses Brought Forward view with errors" should {
    lazy val form = lossesBroughtForwardForm.bind(Map(("option", "")))
    lazy val view = views.lossesBroughtForward(form, postAction, "", TaxYearModel("2015/16", true, "2015/16"), "home", "navTitle")(fakeRequest, applicationMessages)
    lazy val doc = Jsoup.parse(view.body)

    "display an error summary message for the amount" in {
      doc.body.select("#option-error-summary").size shouldBe 1
    }

    "display an error message for the input" in {
      doc.body.select("span.error-notification").size shouldBe 1
    }
  }
}
