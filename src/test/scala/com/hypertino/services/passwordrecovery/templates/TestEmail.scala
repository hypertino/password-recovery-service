package com.hypertino.services.passwordrecovery.templates

import scalatags.Text.all._

class TestEmail($: Value, implicit val l: LanguageRanges) extends Email(
  recipients  = Seq(($("user.email"), $("user.name"))),
  subject = "Hello",
  html = p(
    "Hello ", strong($("user.name")),
    hr,
    "How are you?",
    a(href:=$("site.test-url")+"/abcde")("read more"),
    p("color: ", r("color"))
  )
)

