package flowy.scheduler.server.rest.actions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import flowy.scheduler.entities.TaskType;
import flowy.scheduler.server.data.TaskTypeDAO;
import flowy.scheduler.server.rest.IAction;

public class GetTaskTypesAction implements IAction {

	private TaskTypeDAO m_taskTypeDao = new TaskTypeDAO();
	@Override
	public String processRequest(HttpRequest request, String content, 
			ChannelHandlerContext ctx) {
		List<TaskType> list = m_taskTypeDao.GetAllTaskTypes();
		StringBuilder buffer = new StringBuilder();
		JSONArray jsonlist = new JSONArray();
		for (TaskType obj : list){
			JSONObject map = new JSONObject();
			//jsonlist.put
			//Map map = new HashMap();
			map.put("id", obj.getId());
			map.put("name", obj.getName());
			jsonlist.put(map);
		}

		//JSONObject json = JSONObject.valueToString(jsonlist);
		//return json.toString();
		//return JSONObject.valueToString(jsonlist);
		return jsonlist.toString();
	}
}
