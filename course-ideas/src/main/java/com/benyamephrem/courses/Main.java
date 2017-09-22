package com.benyamephrem.courses;

import com.benyamephrem.courses.model.CourseIdea;
import com.benyamephrem.courses.model.CourseIdeaDAO;
import com.benyamephrem.courses.model.NotFoundException;
import com.benyamephrem.courses.model.SimpleCourseIdeaDAO;
import spark.ModelAndView;
import spark.Request;
import spark.template.handlebars.HandlebarsTemplateEngine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static spark.Spark.*;

public class Main {

    private static final String FLASH_MESSAGE_KEY = "flash_message";

    public static void main(String[] args) {

        //Let's spark know where our files are
        staticFileLocation("/public");

        //Later we can easily switch this with a database implementation
        CourseIdeaDAO dao = new SimpleCourseIdeaDAO();

        //When we don't put a path that means this will happen for every request and response
        //Here we replace req.cookie with req.attribute to untie our code from the cookie username implementation
        before((req, res) -> {
            if(req.cookie("username") != null){
                req.attribute("username", req.cookie("username"));
            }
        });

        //These before statements are filters that check conditions BEFORE fulfilling a request (this is called "Middleware")
        before("/ideas", (req, res) -> {
            //This catches if someone wants to access the page but hasn't logged in yet
            if(req.attribute("username") == null){
                setFlashMessage(req, "You have to log in first!");
                res.redirect("/");
                halt(); //Stops execution of request
            }
        });

        //When something matching this route is fetched, a model and view are created,
        //then they are rendered by the HandlebarsTemplateEngine
        get("/", (req, res) -> {
            Map<String, String> model = new HashMap<>();
            model.put("username", req.attribute("username"));
            model.put("flashMessage", captureFlashMessage(req));
            return new ModelAndView(model, "index.hbs");
        }, new HandlebarsTemplateEngine());

        post("/sign-in", (req, res) -> {
            Map<String, String> model = new HashMap<>();
            //Here we make a model for passing info to the ModelAndView. We make a model map
            //of info we want passed and pull the name identifies a form field from the
            //request URI with queryParams
            String username = req.queryParams("username");
            //Adds the input to the cookies for later use
            res.cookie("username", username);
            model.put("username", username);
            res.redirect("/");
            return null;
        }); //Another PRG refresh (Post Redirect Get)

        get("/ideas", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            model.put("ideas", dao.findAll());
            //If message exists it will pull through, if not nothing happens
            model.put("flashMessage", captureFlashMessage(req));
            return new ModelAndView(model, "ideas.hbs");
        }, new HandlebarsTemplateEngine());

        post("/ideas", (req, res) -> {
            String title = req.queryParams("title");
            CourseIdea courseIdea = new CourseIdea(title, req.attribute("username"));
            dao.add(courseIdea); //Dummy DAO acting like our simple database
            res.redirect("/ideas");
            return null; //We don't return a model and view because we just want to refresh the same page, not load a new one
                         //Called the "PRG" pattern - post redirect get - to refresh a page
        });

        post("/ideas/:slug/vote", (req, res) -> {
            CourseIdea idea = dao.findBySlug(req.params("slug"));
            boolean added = idea.addVoter(req.attribute("username"));
            if(added){
                setFlashMessage(req, "Thanks for your vote!");
            } else{
                setFlashMessage(req, "You already voted!");
            }
            res.redirect("/ideas");
            return null;
        });

        get("/ideas/:slug", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            model.put("idea", dao.findBySlug(req.params("slug")));
            return new ModelAndView(model, "idea.hbs");
        }, new HandlebarsTemplateEngine());

        //Handle certain exceptions that can be thrown, here we handle a 404 error
        exception(NotFoundException.class, (exc, req, res) -> {
            res.status(404);
            HandlebarsTemplateEngine engine = new HandlebarsTemplateEngine();
            String html = engine.render(new ModelAndView(null, "not-found.hbs"));
            res.body(html);
        });
    }

    private static void setFlashMessage(Request req, String message) {
        req.session().attribute(FLASH_MESSAGE_KEY, message); //Empty parameters makes a session, parameters provides session ID
    }

    private static String getFlashMessage(Request req){
        if(req.session(false) == null){
            //If session doesn't exist then there are no flash messages to get
            return null;
        }

        if(!req.session().attributes().contains(FLASH_MESSAGE_KEY)){
            return null;
        }

        return (String) req.session().attribute(FLASH_MESSAGE_KEY);

    }

    private static String captureFlashMessage(Request req){
        String message = getFlashMessage(req);
        if(message != null){
            req.session().removeAttribute(FLASH_MESSAGE_KEY);
        }
        return message;
    }

}
