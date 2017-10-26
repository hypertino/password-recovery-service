/*
 * Copyright (c) 2017 Magomed Abdurakhmanov, Hypertino
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.hypertino.services.passwordrecovery

import java.net.URLEncoder
import java.util.Base64

import com.hypertino.binders.value.{Lst, Null, Obj, Value}
import com.hypertino.hyperbus.Hyperbus
import com.hypertino.hyperbus.model.{Accepted, BadRequest, Conflict, Created, DynamicBody, EmptyBody, ErrorBody, Ok, ResponseBase, Unauthorized}
import com.hypertino.hyperbus.subscribe.Subscribable
import com.hypertino.service.control.api.Service
import com.hypertino.services.passwordrecovery.api.PasswordRecoveriesPost
import com.hypertino.services.passwordrecovery.apiref.authpin.{CreatePin, PinsPost}
import com.hypertino.services.passwordrecovery.apiref.email.{EmailMessage, EmailsPost}
import com.hypertino.services.passwordrecovery.apiref.user.UsersGet
import com.typesafe.config.Config
import com.typesafe.scalalogging.StrictLogging
import monix.eval.Task
import monix.execution.Scheduler
import scaldi.{Injectable, Injector}

import scala.concurrent._
import scala.concurrent.duration.FiniteDuration
import scala.util.Success

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

  def onEmailsPost(implicit p: PasswordRecoveriesPost): Task[Accepted[DynamicBody]] = {
    p.body.email.map { email ⇒
      hyperbus
        .ask(UsersGet(query = Obj.from("email" → email)))
        .flatMap {
          case Ok(DynamicBody(None,Lst(items)), _) ⇒ {
            if (items.isEmpty || items.tail.nonEmpty) {
              Task.raiseError(Conflict(ErrorBody("user-not-found", Some(s"User with '$email' is not found"))))
            }
            else {
              val user = items.head
              val user_id = user.dynamic.user_id.toString
              val pinId = "pwd-" + user_id

              hyperbus
                .ask(PinsPost(CreatePin(pinId, identityKeys=Obj.from("user_id" → user_id, "email" → email))))
                .flatMap { r ⇒
                  val pin = r.body.pin
                  val pin64 = new String(Base64.getEncoder.encode((pinId + ":" + pin).getBytes("UTF-8")), "UTF-8")
                  val emailData = Obj.from("user" → user, "pin" → pin64)
                  hyperbus
                    .ask(EmailsPost(EmailMessage("recovery-password",p.body.language,emailData)))
                    .map { _ ⇒
                      Accepted(EmptyBody)
                    }
                }
            }
          }
        }
    } getOrElse {
      // email is optional, in future there could be different recovery options (phone?)
      Task.raiseError(BadRequest(ErrorBody("email-is-not-specified")))
    }
  }

  override def stopService(controlBreak: Boolean, timeout: FiniteDuration): Future[Unit] = Future {
    handlers.foreach(_.cancel())
    logger.info(s"${getClass.getName} is STOPPED")
  }
}
