package br.com.fences.deicdivecarbackend.roubocarga.converter;

import java.lang.reflect.Type;
import java.util.Collection;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * Responsavel por desconsiderar colecoes nulas ou vazias.
 */
public class ColecaoJsonAdapter implements JsonSerializer<Collection<?>> {

	@Override
	public JsonElement serialize(Collection<?> src, Type typeOfSrc, JsonSerializationContext context) {
	    if (src == null || src.isEmpty())
	    {
	        return null; //-- exclusao de colecao nula
	    }

	    JsonArray array = new JsonArray();
	    for (Object child : src) 
	    {
	    	JsonElement element = context.serialize(child);
	    	array.add(element);
	    }
	    return array;
	}
}