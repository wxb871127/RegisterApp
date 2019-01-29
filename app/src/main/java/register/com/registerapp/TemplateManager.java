package register.com.registerapp;

import java.util.ArrayList;
import java.util.List;

public class TemplateManager {
    List<BaseTemplate> list = new ArrayList<>();
    List<BaseState> stateList = new ArrayList<>();

    public void registerTemplate(BaseTemplate template){
        list.add(template);
    }

    public void registerState(BaseState state){
        stateList.add(state);
    }

    public List<BaseTemplate> getList(){
        return  list;
    }

    public List<BaseState> getStateList(){
        return stateList;
    }
}
