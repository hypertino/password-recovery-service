/*
 * Copyright (c) 2017 Magomed Abdurakhmanov, Hypertino
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.hypertino.services.passwordrecovery

import javax.mail.internet.InternetAddress

import com.hypertino.binders.value.Obj
import com.hypertino.hyperbus.Hyperbus
import com.hypertino.hyperbus.model.MessagingContext
import com.hypertino.hyperbus.subscribe.Subscribable
import com.hypertino.hyperbus.transport.api.ServiceRegistrator
import com.hypertino.hyperbus.transport.registrators.DummyRegistrator
import com.hypertino.service.config.ConfigLoader
import com.hypertino.services.passwordrecovery.apiref.email.EmailsPost
import com.typesafe.config.Config
import monix.execution.Scheduler
import org.jvnet.mock_javamail.Mailbox
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import scaldi.Module

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
  Thread.sleep(500)

  val service = new PasswordRecoveryService()

  override def afterAll() {
    service.stopService(false, 10.seconds).futureValue
    hyperbus.shutdown(10.seconds).runAsync.futureValue
  }

  "PasswordRecoveryService" should "create pin code and send email" in {
    ???
  }
}
