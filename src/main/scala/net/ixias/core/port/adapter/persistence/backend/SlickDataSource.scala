/*
 * This file is part of the IxiaS services.
 *
 * For the full copyright and license information,
 * please view the LICENSE file that was distributed with this source code.
 */

package net.ixias
package core.port.adapter.persistence.backend

import scala.util.Try
import java.sql.Connection
import slick.jdbc.JdbcDataSource
import core.port.adapter.persistence.io.EntityIOActionContext

trait SlickDataSource extends DataSource with SlickDataSourceConfig {

  // --[ TypeDefs ]-------------------------------------------------------------
  /** The type of database source config used by this backend. */
  type DataSource = HikariCPDataSource

  /** The type of the database souce config factory used by this backend. */
  type DataSourceFactory = HikariCPDataSourceFactory

  /** The type of the context used for running repository Actions */
  type Context = EntityIOActionContext

  // --[ Properties ]-----------------------------------------------------------
  /** The database factory */
  lazy val DataSource = new HikariCPDataSourceFactory{}

  // --[ DataSource ]-----------------------------------------------------------
  case class HikariCPDataSource (
    val ds:    com.zaxxer.hikari.HikariDataSource,
    val hconf: com.zaxxer.hikari.HikariConfig
  ) extends JdbcDataSource {

    /** Create a new Connection or get one from the pool */
    def createConnection(): Connection = ds.getConnection()

    /** If this object represents a connection pool managed directly by Slick, close it.
      * Otherwise no action is taken. */
    def close(): Unit = ds.close()
  }

  // --[ Factory ]--------------------------------------------------------------
  /** Factory methods for creating `DatabSouce` instances with using HikariCP. */
  trait HikariCPDataSourceFactory extends DataSourceFactoryDef {
    import com.zaxxer.hikari._
    import DataSourceName.Implicits._

    /** Create a JdbcDataSource from DSN (Database Souce Name) */
    def forDSN(name: String)(implicit ctx: Context): Try[DataSource] =
      for {
        driver <- getDriverClassName(name)
        url    <- getJdbcUrl(name)
      } yield {
        val hconf = new HikariConfig()
        hconf.setDriverClassName(driver)
        hconf.setJdbcUrl(url)
        hconf.setPoolName(name)
        hconf.setMaximumPoolSize(10)

        // Optional properties.
        getUserName(name)                  map hconf.setUsername
        getPassword(name)                  map hconf.setPassword
        getHostSpecReadOnly(name)          map hconf.setReadOnly
        getHostSpecMinIdle(name)           map hconf.setMinimumIdle
        getHostSpecMaxPoolSize(name)       map hconf.setMaximumPoolSize
        getHostSpecConnectionTimeout(name) map hconf.setConnectionTimeout
        getHostSpecIdleTimeout(name)       map hconf.setIdleTimeout

        HikariCPDataSource(new HikariDataSource(hconf), hconf)
      }
  }
}

