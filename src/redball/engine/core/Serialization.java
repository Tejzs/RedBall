package redball.engine.core;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.dyn4j.dynamics.Body;
import redball.engine.entity.ECSWorld;
import redball.engine.entity.GameObject;
import redball.engine.entity.components.Component;
import redball.engine.renderer.Camera;
import redball.engine.renderer.texture.Texture;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class Serialization {
    public static void save() throws IllegalAccessException {
        JsonObject jsonObject = new JsonObject();
        Gson gson = new Gson();
        StringBuilder data = new StringBuilder();
        for (GameObject go : ECSWorld.getGameObjects()) {
            jsonObject.addProperty("name", go.getName());
            JsonObject components = new JsonObject();
            for (Component component : go.getComponents()) {
                JsonObject c = new JsonObject();
                for (Field field : component.getClass().getFields()) {
                    if (!Modifier.isPublic(field.getModifiers())) continue;
                    if (Component.class.isAssignableFrom(field.getType()) || Camera.class.isAssignableFrom(field.getType()) || Texture.class.isAssignableFrom(field.getType()) || Body.class.isAssignableFrom(field.getType()) || GameObject.class.isAssignableFrom(field.getType())) {
                        break;
                    } else {
                        c.add(field.getName(), new Gson().toJsonTree(field.get(component)));
                    }
                }
                components.add(component.getClass().getSimpleName(), c);
            }
            jsonObject.add("component", components);
            data.append(gson.toJson(jsonObject));
        }
        System.out.println(data);
    }
}