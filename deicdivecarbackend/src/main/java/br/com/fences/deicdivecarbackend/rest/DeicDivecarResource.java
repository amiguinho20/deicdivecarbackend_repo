package br.com.fences.deicdivecarbackend.rest;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.log4j.Logger;

import br.com.fences.deicdivecarbackend.roubocarga.negocio.EnderecoAvulsoBO;
import br.com.fences.deicdivecarbackend.roubocarga.negocio.RouboCarga;
import br.com.fences.deicdivecarentidade.enderecoavulso.EnderecoAvulso;
import br.com.fences.fencesutils.conversor.InputStreamParaJson;
import br.com.fences.ocorrenciaentidade.ocorrencia.Ocorrencia;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


@RequestScoped
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DeicDivecarResource {
	
	@Inject
	private transient Logger logger;

	@Inject
	private EnderecoAvulsoBO enderecoAvulsoBO;
	
	@Inject 
	private RouboCarga rouboCarga;
	
	private Gson gson = new GsonBuilder().create();
	
    @GET
    @Path("enderecoAvulso/consultar/{id}")
    public String enderecoAvulsoConsultar(@PathParam("id") String id) 
    {
    	EnderecoAvulso enderecoAvulso = enderecoAvulsoBO.consultar(id);
    	String json = gson.toJson(enderecoAvulso);
    	return json;
    }
    
    @POST
    @Path("enderecoAvulso/contar")
    public String enderecoAvulsoContar(InputStream ipFiltros) 
    {
    	String json = InputStreamParaJson.converter(ipFiltros);
		
    	Map<String, String> mapFiltros = gson.fromJson(json, HashMap.class);
    	int count = enderecoAvulsoBO.contar(mapFiltros);
    	
    	return Integer.toString(count);
    }
    
    @POST
    @Path("enderecoAvulso/pesquisarLazy/{primeiroRegistro}/{registrosPorPagina}")
    public String enderecoAvulsoPesquisarLazy(
    		@PathParam("primeiroRegistro") int primeiroRegistro,
    		@PathParam("registrosPorPagina") int registrosPorPagina,
    		InputStream ipFiltros) 
    {
    	String json = InputStreamParaJson.converter(ipFiltros);
    	Map<String, String> mapFiltros = gson.fromJson(json, HashMap.class);
    	List<EnderecoAvulso> enderecosAvulsos = enderecoAvulsoBO.pesquisarLazy(mapFiltros, primeiroRegistro, registrosPorPagina);
    	json = gson.toJson(enderecosAvulsos);
    	return json;
    }

    @POST
    @Path("enderecoAvulso/pesquisarAtivoPorTipo")
    public String enderecoAvulsoPesquisarAtivoPorTipo(InputStream ipFiltros) 
    {
    	String json = InputStreamParaJson.converter(ipFiltros);
    	List<String> lstFiltros = gson.fromJson(json, List.class);
    	List<EnderecoAvulso> enderecosAvulsos = enderecoAvulsoBO.pesquisarAtivoPorTipo(lstFiltros);
    	json = gson.toJson(enderecosAvulsos);
    	return json;
    }    
    
    
    @PUT
    @Path("enderecoAvulso/adicionar")
    public void enderecoAvulsoAdicionar(InputStream ipFiltros) 
    {
    	String json = InputStreamParaJson.converter(ipFiltros);
    	EnderecoAvulso enderecoAvulso = gson.fromJson(json, EnderecoAvulso.class);
    	enderecoAvulsoBO.adicionar(enderecoAvulso);
    }
    
    @POST
    @Path("enderecoAvulso/substituir")
    public void enderecoAvulsoSubstituir(InputStream ipFiltros) 
    {
    	String json = InputStreamParaJson.converter(ipFiltros);
    	EnderecoAvulso enderecoAvulso = gson.fromJson(json, EnderecoAvulso.class);
    	enderecoAvulsoBO.substituir(enderecoAvulso);
    }
    
    @DELETE
    @Path("enderecoAvulso/remover")
    public void enderecoAvulsoRemover(InputStream ipFiltros) 
    {
    	String json = InputStreamParaJson.converter(ipFiltros);
    	EnderecoAvulso enderecoAvulso = gson.fromJson(json, EnderecoAvulso.class);
    	enderecoAvulsoBO.remover(enderecoAvulso);
    }

    //**********
    @PUT
    @Path("rouboCarga/adicionar")
	public void rouboCargaAdicionar(InputStream ipFiltros)
	{
    	String json = InputStreamParaJson.converter(ipFiltros);
    	Ocorrencia ocorrencia = gson.fromJson(json, Ocorrencia.class);
		rouboCarga.adicionar(ocorrencia);
	}
	
    @POST
    @Path("rouboCarga/agregarPorAno")
	public String rouboCargaAgregarPorAno(InputStream ipFiltros)
	{
    	String json = InputStreamParaJson.converter(ipFiltros);
    	Map<String, String> filtros = gson.fromJson(json, Map.class);
		Map<String, Integer> mapRetorno = rouboCarga.agregarPorAno(filtros);
		json = gson.toJson(mapRetorno);
    	return json;
	}

    @POST
    @Path("rouboCarga/agregarPorComplementar")
	public String rouboCargaAgregarPorComplementar(InputStream ipFiltros)
	{
    	String json = InputStreamParaJson.converter(ipFiltros);
    	Map<String, String> filtros = gson.fromJson(json, Map.class);
    	Map<String, Integer> mapRetorno =  rouboCarga.agregarPorComplementar(filtros);
    	json = gson.toJson(mapRetorno);
    	return json;
	}

    @POST
    @Path("rouboCarga/agregarPorFlagrante")
	public String rouboCargaAgregarPorFlagrante(InputStream ipFiltros)
	{
    	String json = InputStreamParaJson.converter(ipFiltros);
    	Map<String, String> filtros = gson.fromJson(json, Map.class);
    	Map<String, Integer> mapRetorno = rouboCarga.agregarPorFlagrante(filtros);
    	json = gson.toJson(mapRetorno);
    	return json;
	}
	
    @GET
    @Path("rouboCarga/consultar/{id}")
	public String rouboCargaConsultar(@PathParam("id") String id)
	{
		Ocorrencia ocorrencia = rouboCarga.consultar(id);
		String json = gson.toJson(ocorrencia);
		return json;
	}

    @POST
    @Path("rouboCarga/contar")
	public String rouboCargaContar(InputStream ipFiltros)
	{
    	String json = InputStreamParaJson.converter(ipFiltros);
    	Map<String, String> filtros = gson.fromJson(json, Map.class);
		int count = rouboCarga.contar(filtros);
		return Integer.toString(count);
	}
	
    @POST
    @Path("rouboCarga/pesquisarLazy/{primeiroRegistro}/{registrosPorPagina}")
	public String rouboCargaPesquisarLazy(    		
			@PathParam("primeiroRegistro") int primeiroRegistro,
    		@PathParam("registrosPorPagina") int registrosPorPagina,
    		InputStream ipFiltros)
	{
    	String json = InputStreamParaJson.converter(ipFiltros);
    	Map<String, String> filtros = gson.fromJson(json, Map.class);
    	List<Ocorrencia> ocorrencias = rouboCarga.pesquisarLazy(filtros, primeiroRegistro, registrosPorPagina);
    	json = gson.toJson(ocorrencias);
    	return json;
	}

    @GET
    @Path("rouboCarga/pesquisarUltimaDataRegistroNaoComplementar")
	public String rouboCargaPesquisarUltimaDataRegistroNaoComplementar()
	{
		return rouboCarga.pesquisarUltimaDataRegistroNaoComplementar();
	}

    @POST
    @Path("rouboCarga/substituir")
	public void rouboCargaSubstituir(InputStream ipFiltros)
	{
    	String json = InputStreamParaJson.converter(ipFiltros);
    	Ocorrencia ocorrencia = gson.fromJson(json, Ocorrencia.class);
		rouboCarga.substituir(ocorrencia);
	}
    
}
