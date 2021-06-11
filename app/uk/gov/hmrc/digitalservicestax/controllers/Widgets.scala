/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.digitalservicestax.controllers

import java.time.LocalDate

import cats.data.Validated
import cats.implicits._
import enumeratum._
import ltbs.uniform.common.web.{FormField, GenericWebTell, ListingRow, WebInteraction}
import ltbs.uniform.interpreters.playframework.Breadcrumbs
import ltbs.uniform.validation.Rule._
import ltbs.uniform.validation._
import ltbs.uniform._
import play.twirl.api.Html
import play.twirl.api.HtmlFormat.Appendable
import shapeless.tag
import tag.@@
import uk.gov.hmrc.digitalservicestax._
import uk.gov.hmrc.digitalservicestax.config.AppConfig
import uk.gov.hmrc.digitalservicestax.data._
import uk.gov.hmrc.digitalservicestax.frontend.{Kickout, RichAddress}

trait Widgets {

  implicit val appConfig: AppConfig

  implicit val twirlStringField: FormField[Html, String] = twirlStringFields()

  type CustomStringRenderer =
    (List[String], String, ErrorTree, UniformMessages[Html], Option[String]) => Appendable

  def twirlStringFields(
    autoFields: Option[String] = None,
    customRender: CustomStringRenderer = views.html.uniform.string.apply(_,_,_,_,_)
  ): FormField[Html, String] = new FormField[Html, String] {
    def decode(out: Input): Either[ErrorTree, String] =
      out.toStringField().toEither

    def encode(in: String): Input = Input.one(List(in))

    def render(
      pageKey: List[String],
      fieldKey: List[String],
      path: Breadcrumbs,
      data: Input,
      errors: ErrorTree,
      messages: UniformMessages[Html]
    ): Option[Html] = {
      val existingValue: String = data.valueAtRoot.flatMap{_.headOption}.getOrElse("")

      customRender(fieldKey, existingValue, errors, messages, autoFields).some
    }
  }

  def validatedVariant[BaseType](validated: ValidatedType[BaseType])(
    implicit baseForm: FormField[Html, BaseType]
  ): FormField[Html, BaseType @@ validated.Tag] =
    baseForm.simap{x =>
      Either.fromOption(validated.of(x), ErrorMsg("invalid").toTree)
    }{x => x: BaseType}

  def validatedString(
    validated: ValidatedType[String],
    maxLen: Int = Integer.MAX_VALUE
  )(
    implicit baseForm: FormField[Html, String]
  ): FormField[Html, String @@ validated.Tag] =
    baseForm.simap{
      case x if x.trim.isEmpty => Left(ErrorMsg("required").toTree)
      case l if l.length > maxLen => Left(ErrorMsg("length.exceeded").toTree)
      case x => Either.fromOption(validated.of(x), ErrorMsg("invalid").toTree)
    }{x => x: String}

  def inlineOptionString(
    validated: ValidatedType[String],
    maxLen: Int = Integer.MAX_VALUE
  ): FormField[Html, Option[String @@ validated.Tag]] =
    twirlStringField.simap{
      case "" => Right(None)
      case l if l.length > maxLen => Left(ErrorMsg("length.exceeded").toTree)
      case x  => Either.fromOption(
        validated.of(x).map(Some(_)), ErrorMsg("invalid").toTree
      )
    }{
      case None => ""
      case Some(x) => x.toString
    }

  def validatedBigDecimal(
    validated: ValidatedType[BigDecimal],
    maxLen: Int = Integer.MAX_VALUE
  )(
    implicit baseForm: FormField[Html, BigDecimal]
  ): FormField[Html, BigDecimal @@ validated.Tag] =
    baseForm.simap{
      case l if l.precision > maxLen => Left(ErrorMsg("length.exceeded").toTree)
      case x => Either.fromOption(validated.of(x), ErrorMsg("invalid").toTree)
    }{x => x: BigDecimal}

  implicit def postcodeField                  = validatedString(Postcode)(twirlStringFields(
    customRender = views.html.uniform.string(_,_,_,_,_,"form-control postcode")
  ))
  implicit def nesField                       = validatedVariant(NonEmptyString)
  implicit def utrField                       = validatedString(UTR)
  implicit def emailField                     = validatedString(Email, 132)
  implicit def phoneField       = validatedString(PhoneNumber, 24)(twirlStringFields(
    customRender = views.html.uniform.phonenumber.apply _
  ))
  implicit def percentField                   = validatedVariant(Percent)
  implicit def moneyField                     = validatedBigDecimal(Money, 15)
  implicit def accountNumberField             = validatedString(AccountNumber)
  implicit def BuildingSocietyRollNumberField = inlineOptionString(BuildingSocietyRollNumber, 18)
  implicit def accountNameField               = validatedString(AccountName, 35)
  implicit def sortCodeField    = validatedString(SortCode)(twirlStringFields(
    customRender = views.html.uniform.string(_,_,_,_,_,"form-control form-control-1-4")
  ))
  
  implicit def ibanField                      = validatedString(IBAN, 34)
  implicit def companyNameField               = validatedString(CompanyName, 105)
  implicit def mandatoryAddressField          = validatedString(AddressLine, 35)
  implicit def optAddressField                = inlineOptionString(AddressLine, 35)
  implicit def restrictField                  = validatedString(RestrictiveString, 35)

  implicit def optUtrField: FormField[Html, Option[UTR]] = inlineOptionString(UTR)

  implicit val intField: FormField[Html, Int] =
    twirlStringFields().simap(x => 
      {
        Rule.nonEmpty[String].apply(x) andThen
        Transformation.catchOnly[NumberFormatException]("not-a-number")(_.toInt)
      }.toEither
    )(_.toString)

  implicit val floatField: FormField[Html, Float] =
    twirlStringFields(
      customRender = views.html.uniform.string(_,_,_,_,_,"form-control govuk-input--width-4 govuk-input-z-index")
    ).simap(x =>
      {
        Rule.nonEmpty[String].apply(x) andThen
        Transformation.catchOnly[NumberFormatException]("not-a-number")(_.toFloat)
      }.toEither
    )(_.toString)

  implicit val longField: FormField[Html, Long] =
    twirlStringFields().simap(x => 
      {
        Rule.nonEmpty[String].apply(x.replace("%", "")) andThen
        Transformation.catchOnly[NumberFormatException]("not-a-number")(_.toLong)
      }.toEither
    )(_.toString)

  implicit val bigdecimalField: FormField[Html, BigDecimal] = {
    twirlStringFields(
      customRender = views.html.uniform.string(_,_,_,_,_,"govuk-input-money govuk-input--width-10")
    ).simap(x => {
      Rule.nonEmpty[String].apply(x.replace(",", "").replace("£", "")) andThen
        Transformation.catchOnly[NumberFormatException]("not-a-number")(BigDecimal.apply)
    }.toEither
    )(_.toString)
  }

  implicit val twirlBoolField = new FormField[Html, Boolean] {
    val True = true.toString.toUpperCase
    val False = false.toString.toUpperCase

    def decode(out: Input): Either[ErrorTree, Boolean] =
      out.toField[Boolean](
        x => nonEmpty[String].apply(x) andThen ( y => Validated.catchOnly[IllegalArgumentException](y.toBoolean)
          .leftMap(_ => ErrorMsg("invalid").toTree))
      ).toEither

    def encode(in: Boolean): Input = Input.one(List(in.toString))

    def render(
      pageKey: List[String],
      fieldKey: List[String],
      path: Breadcrumbs,
      data: Input,
      errors: ErrorTree,
      messages: UniformMessages[Html]
    ): Option[Html] = {
      val options = if (pageKey.contains("about-you") || pageKey.contains("user-employed")) List(False, True) else List(True, False)
      val existingValue = data.toStringField().toOption
      views.html.uniform.radios(fieldKey,
        options,
        existingValue,
        errors,
        messages).some
    }
  }

  implicit val twirlCountryCodeField = new FormField[Html, CountryCode] {

    override def render(
      pageKey: List[String],
      fieldKey: List[String],
      breadcrumbs: Breadcrumbs,
      data: Input,
      errors: ErrorTree,
      messages: UniformMessages[Html]): Option[Html] =
      views.html.helpers.country_select(
        fieldKey.mkString("."),
        data.values.flatten.headOption,
        errors.nonEmpty,
        messages
      ).some

    override def encode(in: CountryCode): Input =
      validatedVariant(CountryCode).encode(in)

    override def decode(out: Input): Either[ErrorTree, CountryCode] =
      validatedVariant(CountryCode).decode(out)

  }

  implicit val twirlDateField: FormField[Html, LocalDate] =
    new FormField[Html, LocalDate] {

      def decode(out: Input): Either[ErrorTree, LocalDate] = {

      def stringAtKey(key: String): Validated[List[String], String] =
        Validated.fromOption(
          out.valueAt(key).flatMap{_.find(_.trim.nonEmpty)},
          List(key)
        )

      (
        stringAtKey("year"),
        stringAtKey("month"),
        stringAtKey("day")
      ).tupled
       .leftMap{x => ErrorMsg(x.reverse.mkString("-and-") + ".empty").toTree}
       .toEither
       .flatMap{ case (ys,ms,ds) =>

         val asNumbers: Either[Exception, (Int,Int,Int)] =
           Either.catchOnly[NumberFormatException]{
             (ys.toInt, ms.toInt, ds.toInt)
           }

         asNumbers.flatMap { case (y,m,d) =>
           Either.catchOnly[java.time.DateTimeException]{
             LocalDate.of(y,m,d)
           }
         }.leftMap(_ => ErrorTree.oneErr(ErrorMsg("not-a-date")))
       }
    }

      def encode(in: LocalDate): Input = Map(
        List("year") → in.getYear(),
        List("month") → in.getMonthValue(),
        List("day") → in.getDayOfMonth()
      ).mapValues(_.toString.pure[List])

      def render(
        pageKey: List[String],        
        fieldKey: List[String],
        path: Breadcrumbs,
        data: Input,
        errors: ErrorTree,
        messages: UniformMessages[Html]
      ): Option[Html] = {
        views.html.uniform.date(
          fieldKey,
          data,
          errors,
          messages
        ).some
      }
    }

  implicit def twirlUKAddressField[T](
    implicit gen: shapeless.LabelledGeneric.Aux[UkAddress,T],
    ffhlist: FormField[Html, T]
  ): FormField[Html, UkAddress] = new FormField[Html, UkAddress] {

    def decode(out: Input): Either[ErrorTree, UkAddress] = ffhlist.decode(out).map(gen.from)
    def encode(in: UkAddress): Input = ffhlist.encode(gen.to(in))

    def render(
      pagekey: List[String],
      fieldKey: List[String],      
      path: Breadcrumbs,
      data: Input,
      errors: ErrorTree,
      messages: UniformMessages[Html]
    ): Option[Html] = {
      // TODO pass thru fieldKey
      views.html.uniform.address(
        fieldKey,
        data,
        errors,
        messages,
        "UkAddress"
      ).some
    }
  }

  implicit def twirlForeignAddressField[T](
    implicit gen: shapeless.LabelledGeneric.Aux[ForeignAddress,T],
    ffhlist: FormField[Html, T]
  ): FormField[Html, ForeignAddress] = new FormField[Html, ForeignAddress] {

    def decode(out: Input): Either[ErrorTree, ForeignAddress] = ffhlist.decode(out).map(gen.from)
    def encode(in: ForeignAddress): Input = ffhlist.encode(gen.to(in))

    def render(
      pagekey: List[String],
      fieldKey: List[String],
      path: Breadcrumbs,
      data: Input,
      errors: ErrorTree,
      messages: UniformMessages[Html]
    ): Option[Html] = {
      views.html.uniform.address(
        fieldKey,
        data,
        errors,
        messages
      ).some
    }
  }

  implicit def enumeratumField[A <: EnumEntry](implicit enum: Enum[A]): FormField[Html, A] =
    new FormField[Html, A] {

      def decode(out: Input): Either[ErrorTree,A] = {out.toField[A](x =>
        nonEmpty[String].apply(x) andThen
          ( y => Validated.catchOnly[NoSuchElementException](enum.withName(y)).leftMap(_ => ErrorTree.oneErr(ErrorMsg("invalid"))))
      )}.toEither

      def encode(in: A): Input = Input.one(List(in.entryName))
      def render(
        pageKey: List[String],
        fieldKey: List[String],        
        path: Breadcrumbs,
        data: Input,
        errors: ErrorTree,
        messages: UniformMessages[Html]
      ): Option[Html] = {
        val options = enum.values.map{_.entryName}
        val existingValue = decode(data).map{_.entryName}.toOption
        views.html.uniform.radios(
          fieldKey,
          options,
          existingValue,
          errors,
          messages
        ).some
      }
    }

  implicit def enumeratumSetField[A <: EnumEntry](implicit enum: Enum[A]): FormField[Html, Set[A]] =
    new FormField[Html, Set[A]] {

      def decode(out: Input): Either[ErrorTree,Set[A]] = {
        val i: List[String] = out.valueAtRoot.getOrElse(Nil)
        val r: List[Either[ErrorTree, A]] = i.map{x =>
          Either.catchOnly[NoSuchElementException](enum.withName(x))
            .leftMap(_ => ErrorTree.oneErr(ErrorMsg("invalid")))
        }
        r.sequence.map{_.toSet}
      }

      // Members declared in ltbs.uniform.common.web.Codec
      def encode(in: Set[A]): Input =
        Map(Nil -> in.toList.map{_.entryName})

      // Members declared in ltbs.uniform.common.web.FormField
      def render(
        pageKey: List[String],
        fieldKey: List[String],
        breadcrumbs: Breadcrumbs,
        data: Input,
        errors: ErrorTree,
        messages: UniformMessages[Html]
      ): Option[Html] = {
        val options = enum.values.map{_.entryName}
        val existingValues: Set[String] = decode(data).map{_.map{_.entryName}}.getOrElse(Set.empty)
        views.html.uniform.checkboxes(
          fieldKey,
          options,
          existingValues,
          errors,
          messages
        ).some
      }

    }

  implicit val addressTell = new GenericWebTell[Html, Address] {
    override def render(in: Address, key: String, messages: UniformMessages[Html]): Html =
      Html(
        s"<div id='${key}--sfer-content'>" +
        s"<p>${in.lines.map{x => s"<span class='govuk-body-m'>${x.escapeHtml}</span>"}.mkString("<br/>")}</p>" +
        "</div>"
      )
  }

  implicit val kickoutTell = new GenericWebTell[Html, Kickout] {
    override def render(in: Kickout, key: String, messages: UniformMessages[Html]): Html =
      views.html.end.kickout(key)(messages, appConfig)
  }

  implicit val groupCoTell = new GenericWebTell[Html, GroupCompany] {
    override def render(in: GroupCompany, key: String, messages: UniformMessages[Html]): Html =
      Html("")
  }

  implicit val companyTell = new GenericWebTell[Html, Company] {
    override def render(in: Company, key: String, messages: UniformMessages[Html]): Html =
      Html(
        s"<p class='govuk-body-l' id='${key}--sfer-content'>" +
          s"${in.name.toString.escapeHtml}<br>" +
          s"<span class='govuk-body-m'>" +
          s"${in.address.lines.map{_.escapeHtml}.mkString("<br>")}" +
          s"</span>" +
          "</p>"
      )
  }

  implicit val booleanTell = new GenericWebTell[Html, Boolean] {
    override def render(in: Boolean, key: String, messages: UniformMessages[Html]): Html =
      Html(in.toString)
  }

}
