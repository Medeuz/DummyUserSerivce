package com.medeuz.userservice

import cats.effect.Effect
import io.circe.Json
import org.http4s.{HttpService, Response, UrlForm}
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl

import scala.collection.mutable

class UserService[F[_]: Effect] extends Http4sDsl[F] {
  // Temp storage to imitate database
  val userDatabase = new mutable.HashMap[String, String]()

  object Methods {
    val hello = "hello"
    val signIn = "signIn"
    val signUp = "signUp"
    val passwordRecovery = "passwordRecovery"
  }
  object Keys {
    val login = "login"
    val password = "password"
  }

  val service: HttpService[F] = {
    HttpService[F] {
      case request @ POST -> Root / Methods.signIn => request.decode[UrlForm] { handleSignIn }
      case request @ POST -> Root / Methods.signUp => request.decode[UrlForm] { handleSignUp }
      case GET -> Root / Methods.hello / name =>
        Ok(Json.obj("message" -> Json.fromString(s"Hello, $name")))
    }
  }

  def handleSignIn(data: UrlForm): F[Response[F]] = {
    val login = data.values.getOrElse(Keys.login, Seq.empty).mkString
    val password = data.values.getOrElse(Keys.password, Seq.empty).mkString
    if (login.isEmpty || password.isEmpty) {
      BadRequest(Json.obj("error" -> Json.fromString("Invalid data, access denied")))
    } else if (isRegisteredUser(login) && userDatabase(login) == password) {
      Ok(Json.obj("message" -> Json.fromString(s"Hi, $login, you are authorized")))
    } else {
      BadRequest(Json.obj("message" -> Json.fromString(s"Hi, wrong login or password")))
    }
  }

  def handleSignUp(data: UrlForm): F[Response[F]] = {
    val login = data.values.getOrElse(Keys.login, Seq.empty).mkString
    val password = data.values.getOrElse(Keys.password, Seq.empty).mkString
    if (login.isEmpty || password.isEmpty) {
      BadRequest(Json.obj("error" -> Json.fromString("Invalid data, sign up failed")))
    } else if (isRegisteredUser(login)) {
      BadRequest(Json.obj("error" -> Json.fromString("Such user already exists")))
    } else {
      userDatabase.put(login, password)
      Ok(Json.obj("message" -> Json.fromString(s"Hi, your login is  {$login} and your password is {$password}")))
    }
  }

  def isRegisteredUser(login: String): Boolean = userDatabase.contains(login)
}
