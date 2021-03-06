package io.sdev.blog.routes

import cats.implicits._
import io.circe.Decoder
import io.circe.generic.semiauto._
import io.circe.syntax._
import io.sdev.blog.services._
import io.sdev.blog.models._
import cats.effect.kernel._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import io.sdev.authority.client.AuthorityClient
import io.circe.Encoder
import io.circe.Json
import io.sdev.authority.models.user.DomainUser

class ArticleRoutes[F[_]: Async](articleService: ArticleService[F], authService: AuthService[F])
    extends Http4sDsl[F]
    with Routes[F] {
  import ArticleRoutes._
  def routes: HttpRoutes[F] = authorizedGETRoutes <+> authService.middleware(authorizedPOSTRoutes)

  private def authorizedGETRoutes: HttpRoutes[F] = {
    HttpRoutes.of {
      case GET -> Root / IntVar(id) =>
        for {
          art <- articleService.findById(Article.Id(id))
          resp <- art.map(a => Ok(a.asJson)).getOrElse(NotFound())
        } yield resp

      case GET -> Root =>
        Ok(articleService.getAll)
    }
  }

  private def authorizedPOSTRoutes: AuthedRoutes[DomainUser, F] = {
    AuthedRoutes.of { case ctxReq @ POST -> Root as user =>
      Ok(for {
        article <- ctxReq.req.as[ArticleCreation]
        id <- articleService.insert(article.title, article.body)
      } yield id)
    }
  }

}

object ArticleRoutes {
  case class ArticleCreation(title: String, body: String)
  object ArticleCreation {
    given Decoder[ArticleCreation] = deriveDecoder
    given entitdyDecoder[F[_]: Concurrent]: EntityDecoder[F, ArticleCreation] = jsonOf[F, ArticleCreation]
  }
}
