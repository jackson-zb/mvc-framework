package ink.zhaibo.mvc.app.controller;

import ink.zhaibo.mvc.app.service.IIndexService;
import ink.zhaibo.mvc.framework.annotations.Autowired;
import ink.zhaibo.mvc.framework.annotations.Controller;
import ink.zhaibo.mvc.framework.annotations.RequestMapping;
import ink.zhaibo.mvc.framework.annotations.RequestParam;

@Controller
@RequestMapping(value = "index")
public class IndexController {

    @Autowired
    private IIndexService indexService;

    @RequestMapping("/")
    public String index(@RequestParam String name) {
        return indexService.index();
    }

}
