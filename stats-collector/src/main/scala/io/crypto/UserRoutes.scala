package io.crypto

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route

import scala.concurrent.Future
import io.crypto.UserRegistry._
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout
import de.heikoseeberger.akkahttpziojson.ZioJsonSupport._
import akka.http.scaladsl.model.RequestEntity
import scala.jdk.DurationConverters._
import scala.concurrent.duration._
import java.util.concurrent.TimeUnit
class UserRoutes(userRegistry: ActorRef[UserRegistry.Command], routeConfig: CollectorConfig.RouteConfig)(implicit val system: ActorSystem[_]) {

  implicit private val timeout = Timeout.create(FiniteDuration(routeConfig.askTimeout.toMillis, TimeUnit.MILLISECONDS).toJava)

  def getUsers(): Future[Users] = userRegistry.ask(GetUsers)
  def getUser(name: String): Future[GetUserResponse] = userRegistry.ask(GetUser(name, _))
  def createUser(user: User): Future[ActionPerformed] = userRegistry.ask(CreateUser(user, _))
  def deleteUser(name: String): Future[ActionPerformed] = userRegistry.ask(DeleteUser(name, _))

  val userRoutes: Route =
    pathPrefix("users") {
      concat(
        pathEnd {
          concat(
            get {
              complete(getUsers())
            },
            post {
              entity(as[User]) { user =>
                onSuccess(createUser(user)) { performed =>
                  complete((StatusCodes.Created, performed))
                }
              }
            }
          )
        },
        path(Segment) { name =>
          concat(
            get {
              rejectEmptyResponse {
                onSuccess(getUser(name)) { response =>
                  complete(response.maybeUser)
                }
              }
            },
            delete {
              onSuccess(deleteUser(name)) { performed =>
                complete((StatusCodes.OK, performed))
              }
            }
          )
        }
      )
    }
}