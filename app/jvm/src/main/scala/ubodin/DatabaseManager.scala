package ubodin
import scalikejdbc._

object DatabaseManager {
  def connectDatabases() : Unit = {
    Class.forName("org.sqlite.JDBC")
    val settings = ConnectionPoolSettings(
      initialSize = 5,
      maxSize = 20,
      //serverTimezone  = "EST",
      connectionTimeoutMillis = 3000L,
      validationQuery = "select 1 ")
    
    val path = java.nio.file.Paths.get("mimir_community_clean.db").toString
    val dbUrl = "jdbc:sqlite:"+path
    
    
    // all the connections are released, old connection pool will be abandoned
    ConnectionPool.add("mimir_community_server", dbUrl, "", "", settings)
  
  }
}