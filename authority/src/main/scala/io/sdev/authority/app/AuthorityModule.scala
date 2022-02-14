package io.sdev.authority.app

import io.sdev.authority.routes._
import io.sdev.authority.services._
import cats.effect.kernel.Sync
import doobie.util.transactor.Transactor
import io.sdev.authority.daos.UserDao
import cats.effect.kernel.Async

trait Module[F[_]] {
  val userRoutes: Routes[F]
}

class AuthorityModule[F[_]: Async](transactor: Transactor[F]) extends Module[F] {
  val userDao: UserDao = UserDao()
  val userService: UserService[F] = UserService[F](userDao, transactor)
  val userRoutes: UserRoutes[F] = UserRoutes[F](userService)
}

object AuthorityModule {
  def make[F[_]: Async](transactor: Transactor[F]) = new AuthorityModule(transactor)
}