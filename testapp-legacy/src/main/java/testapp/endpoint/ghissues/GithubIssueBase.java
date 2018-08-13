package testapp.endpoint.ghissues;


import act.controller.Controller;
import act.controller.annotation.TemplateContext;
import act.controller.annotation.UrlContext;

@UrlContext("/gh")
@TemplateContext("/gh")
public class GithubIssueBase extends Controller.Base {
}
