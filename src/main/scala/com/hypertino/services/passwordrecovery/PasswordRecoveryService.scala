/*
 * Copyright (c) 2017 Magomed Abdurakhmanov, Hypertino
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.hypertino.services.passwordrecovery

import com.hypertino.binders.value.{Null, Value}
import com.hypertino.hyperbus.Hyperbus
import com.hypertino.hyperbus.model.{Created, DynamicBody}
import com.hypertino.hyperbus.subscribe.Subscribable
import com.hypertino.service.control.api.Service
import com.hypertino.services.passwordrecovery.api.PasswordRecoveriesPost
import com.typesafe.config.Config
import com.typesafe.scalalogging.StrictLogging
import monix.eval.Task
import monix.execution.Scheduler
import scaldi.{Injectable, Injector}

import scala.concurrent._
import scala.concurrent.duration.FiniteDuration

case class PasswordRecoveryServiceConfig(
                              smtpHost: String,
                              smtpPort: Option[Int],
                              smtpUser: Option[String],
                              smtpPassword: Option[String],
                              sender: Option[String],
                              senderName: Option[String],
                              templateData: Value = Null
                             )

class PasswordRecoveryService(implicit val injector: Injector) extends Service with Injectable with Subscribable with StrictLogging {
  private implicit val scheduler = inject[Scheduler]
  private val hyperbus = inject[Hyperbus]

  import com.hypertino.binders.config.ConfigBinders._
  val config = inject[Config].read[PasswordRecoveryServiceConfig]("email")

  logger.info(s"${getClass.getName} is STARTED")

  private val handlers = hyperbus.subscribe(this, logger)

  def onEmailsPost(implicit p: PasswordRecoveriesPost): Task[Created[DynamicBody]] = ???

  override def stopService(controlBreak: Boolean, timeout: FiniteDuration): Future[Unit] = Future {
    handlers.foreach(_.cancel())
    logger.info(s"${getClass.getName} is STOPPED")
  }
}
