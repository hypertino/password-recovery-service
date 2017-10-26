/*
 * Copyright (c) 2017 Magomed Abdurakhmanov, Hypertino
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.hypertino.services.passwordrecovery

import java.util.Base64

import com.hypertino.binders.value.{Lst, Null, Obj}
import com.hypertino.hyperbus.Hyperbus
import com.hypertino.hyperbus.model.{Accepted, Conflict, Created, DynamicBody, EmptyBody, MessagingContext, Ok}
import com.hypertino.hyperbus.subscribe.Subscribable
import com.hypertino.hyperbus.transport.api.ServiceRegistrator
import com.hypertino.hyperbus.transport.registrators.DummyRegistrator
import com.hypertino.service.config.ConfigLoader
import com.hypertino.services.passwordrecovery.api.{InitPasswordRecovery, PasswordRecoveriesPost}
import com.hypertino.services.passwordrecovery.apiref.authpin.{NewPin, PinsPost}
import com.hypertino.services.passwordrecovery.apiref.email.EmailsPost
import com.hypertino.services.passwordrecovery.apiref.user.UsersGet
import com.typesafe.config.Config
import monix.eval.Task
import monix.execution.Scheduler
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import scaldi.Module

import scala.collection.mutable
import scala.concurrent.duration._

class PasswordRecoveryServiceSpec extends FlatSpec with Module with BeforeAndAfterAll with ScalaFutures with Matchers with Subscribable {
  override implicit val patienceConfig = PatienceConfig(timeout = scaled(Span(3, Seconds)))
  implicit val scheduler = monix.execution.Scheduler.Implicits.global
  implicit val mcx = MessagingContext.empty
  bind [Config] to ConfigLoader()
  bind [Scheduler] to scheduler
  bind [Hyperbus] to injected[Hyperbus]
  bind [ServiceRegistrator] to DummyRegistrator

  val hyperbus = inject[Hyperbus]
  hyperbus.subscribe(this)
  Thread.sleep(500)

  val service = new PasswordRecoveryService()
  val emails = mutable.ArrayBuffer[EmailsPost]()

  override def afterAll() {
    service.stopService(false, 10.seconds).futureValue
    hyperbus.shutdown(10.seconds).runAsync.futureValue
  }

  def onUsersGet(implicit r: UsersGet) = Task.eval { Ok(DynamicBody(
      if (r.headers.hrl.query.dynamic.email.toString == "john@example.net") {
        Lst.from(Obj.from("user_id" → "100500", "first_name" → "John", "last_name" → "Malkovich", "email" → "john@example.net"))
      }
      else {
        Lst.empty
      }
    ))
  }

  def onPinsPost(implicit r: PinsPost) = Task.eval(Created(NewPin("123", r.body.pinId)))

  def onEmailsPost(implicit r: EmailsPost) = Task.eval {
    emails += r
    Accepted(EmptyBody)
  }

  "PasswordRecoveryService" should "create pin code and send email" in {
    val r = hyperbus
      .ask(PasswordRecoveriesPost(InitPasswordRecovery(email=Some("john@example.net"))))
      .runAsync
      .futureValue

    r shouldBe a[Accepted[_]]
    emails.size shouldBe 1
    val email = emails.head
    emails.clear

    email.body.template shouldBe "recovery-password-email"
    val pin = email.body.data.dynamic.pin.toString
    val s = new String(Base64.getDecoder.decode(pin.getBytes("UTF-8")), "UTF-8")
    s should startWith("pwd-100500:")

    email.body.data.dynamic.user.dynamic.email.toString shouldBe "john@example.net"
  }

  it should "conflict if no such user is found" in {
    val r = hyperbus
      .ask(PasswordRecoveriesPost(InitPasswordRecovery(email=Some("me@example.net"))))
      .runAsync
      .failed
      .futureValue

    r shouldBe a[Conflict[_]]
  }
}
