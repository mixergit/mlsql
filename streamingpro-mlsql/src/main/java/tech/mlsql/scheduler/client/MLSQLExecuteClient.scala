package tech.mlsql.scheduler.client

import java.nio.charset.Charset

import net.sf.json.JSONObject
import org.apache.http.client.fluent.{Form, Request}
import org.apache.http.util.EntityUtils
import tech.mlsql.common.utils.path.PathFun
import tech.mlsql.scheduler.JobNode

/**
  * 2019-09-05 WilliamZhu(allwefantasy@gmail.com)
  */
class MLSQLSchedulerClient[T <% Ordered[T]](
                                             consoleUrl: String,
                                             owner: String,
                                             auth_secret: String
                                           ) extends ExecutorClient[T] {
  override def execute(job: JobNode[T]): Unit = {
    val script = Request.Get(PathFun(consoleUrl).add(s"/api_v1/script_file/get?id=${job.id}").toPath)
      .connectTimeout(60 * 1000)
      .socketTimeout(10 * 60 * 1000).addHeader("access-token", auth_secret)
      .execute().returnContent().asString()
    val scriptContent = JSONObject.fromObject(script).getString("content")

    val res = Request.Post(PathFun(consoleUrl).add("/api_v1/run/script").toPath).
      connectTimeout(60 * 1000).socketTimeout(12 * 60 * 60 * 1000).
      addHeader("access-token", auth_secret).
      bodyForm(Form.form().add("sql", scriptContent).
        add("owner", owner).build(), Charset.forName("utf8"))
      .execute().returnResponse()
    job.isExecuted = job.isExecuted ++ Seq(true)
    if (res.getStatusLine.getStatusCode == 200) {
      job.isSuccess = job.isSuccess ++ Seq(true)
    } else {
      job.isSuccess = job.isSuccess ++ Seq(false)
    }
    try {
      job.msg = job.msg ++ Seq(new String(EntityUtils.toByteArray(res.getEntity), Charset.forName("utf8")))
    } catch {
      case
        e: Exception =>
        job.msg = job.msg ++ Seq(e.getMessage)
    }

  }
}
