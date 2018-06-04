package ink.zhaibo.mvc.app.service;

import ink.zhaibo.mvc.framework.annotations.Service;

@Service(value = "indexService")
public class IndexServiceImpl implements IIndexService {

    @Override
    public String index() {
        return "ok";
    }
}
