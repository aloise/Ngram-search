package controllers

import play.api._
import play.api.mvc._
import global.Global
import views.html.defaultpages.badRequest
import play.api.libs.json.Json
import play.api.libs.concurrent.Execution.Implicits._
import actors.messages.{SearchRowsResult, SearchRowsRequest}
import akka.pattern.ask
import scala.concurrent.Future
import akka.util.Timeout
import scala.concurrent.duration._

class Application extends Controller {

  // search timeout
  implicit val timeout = Timeout(60.seconds)

  def index = Action {
    Ok("Hello World!")
  }
  
  def search(field:String, query:String, limit:Int = 1000) = Action.async { request =>
    
    val q = query.trim.toLowerCase
    
    if( !q.isEmpty ){
    	Global.getSearchProvider(field) match {
        case Some(actor) =>
          actor.ask(SearchRowsRequest(query, limit))(timeout).
            mapTo[SearchRowsResult].
            map(r => Ok(Json.toJson(r.ids)))

        case None =>
          Future.successful(BadRequest(Json.obj("error" -> "search_field_was_not_found")))
      }
    	
    } else {
      Future.successful( BadRequest(Json.obj( "error" -> "search_string_is_empty")) )
    }
    

  }
  
  def update(id:Int) = Action {
    val result = false
    Ok(Json.toJson(result))
    
  }
  
  def updateAll( ) = Action {
    
    val result = true
    Ok(Json.toJson(result))
  }
  
  
  
}