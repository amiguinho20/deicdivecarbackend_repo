package br.com.fences.deicdivecarbackend.rest;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import br.com.fences.deicdivecarbackend.indiciado.negocio.IndiciadoBO;
import br.com.fences.deicdivecarbackend.roubocarga.negocio.ControleOcorrenciaBO;
import br.com.fences.deicdivecarbackend.roubocarga.negocio.EnderecoAvulsoBO;
import br.com.fences.deicdivecarbackend.roubocarga.negocio.RouboCarga;
import br.com.fences.deicdivecarentidade.enderecoavulso.EnderecoAvulso;
import br.com.fences.deicdivecarentidade.indiciado.Indiciado;
import br.com.fences.fencesutils.conversor.InputStreamParaJson;
import br.com.fences.fencesutils.conversor.converter.ColecaoJsonAdapter;
import br.com.fences.fencesutils.conversor.converter.Converter;
import br.com.fences.ocorrenciaentidade.controle.ControleOcorrencia;
import br.com.fences.ocorrenciaentidade.ocorrencia.Ocorrencia;


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
	private ControleOcorrenciaBO controleOcorrenciaBO;

	@Inject
	private IndiciadoBO indiciadoBO;

	
	@Inject 
	private RouboCarga rouboCarga;

	@Inject
	private Converter<Ocorrencia> converterOcorrencia;
	
	@Inject
	private Converter<EnderecoAvulso> converterEnderecoAvulso;
	
	@Inject
	private Converter<ControleOcorrencia> converterControleOcorrencia;
	
	@Inject
	private Converter<Indiciado> converterIndiciado;

	
	private Gson gson = new GsonBuilder()
			.registerTypeHierarchyAdapter(Collection.class, new ColecaoJsonAdapter())
			.create();
	
    @GET
    @Path("enderecoAvulso/consultar/{id}")
    public String enderecoAvulsoConsultar(@PathParam("id") String id) 
    {
    	EnderecoAvulso enderecoAvulso = enderecoAvulsoBO.consultar(id);
    	String json = converterEnderecoAvulso.paraJson(enderecoAvulso);
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
    	json = converterEnderecoAvulso.paraJson(enderecosAvulsos);
    	return json;
    }

    @POST
    @Path("enderecoAvulso/pesquisarAtivoPorTipo")
    public String enderecoAvulsoPesquisarAtivoPorTipo(InputStream ipFiltros) 
    {
    	String json = InputStreamParaJson.converter(ipFiltros);
    	List<String> lstFiltros = gson.fromJson(json, List.class); 
    	List<EnderecoAvulso> enderecosAvulsos = enderecoAvulsoBO.pesquisarAtivoPorTipo(lstFiltros);
    	json = converterEnderecoAvulso.paraJson(enderecosAvulsos);
    	return json;
    }    
    
    
    @PUT
    @Path("enderecoAvulso/adicionar")
    public void enderecoAvulsoAdicionar(InputStream ipFiltros) 
    {
    	String json = InputStreamParaJson.converter(ipFiltros);
    	EnderecoAvulso enderecoAvulso = converterEnderecoAvulso.paraObjeto(json, EnderecoAvulso.class);
    	enderecoAvulsoBO.adicionar(enderecoAvulso);
    }
    
    @PUT
    @Path("enderecoAvulso/adicionarLote")
    public void enderecoAvulsoAdicionarLote(InputStream ipFiltros) 
    {
    	String json = InputStreamParaJson.converter(ipFiltros);
		Type collectionType = new TypeToken<List<EnderecoAvulso>>(){}.getType();
    	List<EnderecoAvulso> enderecosAvulsos = (List<EnderecoAvulso>) converterEnderecoAvulso.paraObjeto(json, collectionType);
    	enderecoAvulsoBO.adicionar(enderecosAvulsos);
    }
    
    
    @POST
    @Path("enderecoAvulso/substituir")
    public void enderecoAvulsoSubstituir(InputStream ipFiltros) 
    {
    	String json = InputStreamParaJson.converter(ipFiltros);
    	EnderecoAvulso enderecoAvulso = converterEnderecoAvulso.paraObjeto(json, EnderecoAvulso.class);
    	enderecoAvulsoBO.substituir(enderecoAvulso);
    }
    
    @DELETE
    @Path("enderecoAvulso/remover/{id}")
    public void enderecoAvulsoRemover(@PathParam("id") String id) 
    {
    	enderecoAvulsoBO.remover(id);
    }

    //**********
    @PUT
    @Path("rouboCarga/adicionar")
	public String rouboCargaAdicionar(InputStream ipFiltros)
	{
    	String json = InputStreamParaJson.converter(ipFiltros);
    	Ocorrencia ocorrencia = converterOcorrencia.paraObjeto(json, Ocorrencia.class);
		ocorrencia = rouboCarga.adicionar(ocorrencia);
		json = converterOcorrencia.paraJson(ocorrencia);
		return json;
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
		String json = converterOcorrencia.paraJson(ocorrencia);
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
    	json = converterOcorrencia.paraJson(ocorrencias);
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
    	Ocorrencia ocorrencia = converterOcorrencia.paraObjeto(json, Ocorrencia.class);
		rouboCarga.substituir(ocorrencia);
	}
    
    @GET
    @Path("rouboCarga/pesquisarPrimeiraDataRegistro")
	public String rouboCargaPesquisarPrimeiraDataRegistro()
	{
		return rouboCarga.pesquisarPrimeiraDataRegistro();
	}

    @GET
    @Path("rouboCarga/pesquisarUltimaDataRegistro")
	public String rouboCargaUltimaPrimeiraDataRegistro()
	{
		return rouboCarga.pesquisarUltimaDataRegistro();
	}
    
    @GET
    @Path("rouboCarga/listarAnos")
	public String rouboCargaListarAnos()
	{
		List<String> anos = rouboCarga.listarAnos();
		String json = gson.toJson(anos);
		return json;
	}
 
    @GET
    @Path("rouboCarga/listarDelegacias")
	public String rouboCargaListarDelegacias()
	{
		Map<String, String> delegacias = rouboCarga.listarDelegacias();
		String json = gson.toJson(delegacias);
		return json;
	}
    
    @GET
    @Path("rouboCarga/listarTipoObjetos")
	public String rouboCargaListarTipoObjetos()
	{
		Map<String, String> tipos = rouboCarga.listarTipoObjetos();
		String json = gson.toJson(tipos);
		return json;
	}
    
    
    //--- CONTROLE OCORRENCIA
    
    
    @GET
    @Path("controleOcorrencia/pesquisarUltimaDataRegistroNaoComplementar")
	public String controleOcorrenciaPesquisarUltimaDataRegistroNaoComplementar()
	{
		return controleOcorrenciaBO.pesquisarUltimaDataRegistroNaoComplementar();
	}
    
    @PUT
    @Path("controleOcorrencia/adicionar")
	public void controleOcorrenciaAdicionar(InputStream ipFiltros)
	{
    	String json = InputStreamParaJson.converter(ipFiltros);
    	ControleOcorrencia controleOcorrencia = converterControleOcorrencia.paraObjeto(json, ControleOcorrencia.class);
    	controleOcorrenciaBO.adicionar(controleOcorrencia);
	}
    
    @GET
    @Path("controleOcorrencia/pesquisarProcessarReprocessar")
	public String controleOcorrenciaPesquisarProcessarReprocessar()
	{
		Set<ControleOcorrencia> controleOcorrencias = controleOcorrenciaBO.pesquisarProcessarReprocessar();
		String json = converterControleOcorrencia.paraJson(controleOcorrencias);
		return json;
	}

    @GET
    @Path("controleOcorrencia/pesquisarIndiciadosProcessarReprocessar")
	public String controleOcorrenciaPesquisarIndiciadosProcessarReprocessar()
	{
		Set<ControleOcorrencia> controleOcorrencias = controleOcorrenciaBO.pesquisarIndiciadosProcessarReprocessar();
		String json = converterControleOcorrencia.paraJson(controleOcorrencias);
		return json;
	}

    
    @POST
    @Path("controleOcorrencia/substituir")
	public void controleOcorrenciaSubstituir(InputStream ipFiltros)
	{
    	String json = InputStreamParaJson.converter(ipFiltros);
    	ControleOcorrencia controleOcorrencia = converterControleOcorrencia.paraObjeto(json, ControleOcorrencia.class);
    	controleOcorrenciaBO.substituir(controleOcorrencia);
	}
    
    
    ///---- INDICIADO
    @GET
    @Path("indiciado/consultar/{nome}/{nomeDaMae}/{rg}")
	public String indiciadoConsultar(@PathParam("nome") String nome,
			@PathParam("nomeDaMae") String nomeDaMae,
			@PathParam("rg") String rg)
	{
    	Indiciado indiciado = new Indiciado();
    	indiciado.setNome(nome);
    	indiciado.setNomeDaMae(nomeDaMae);
    	indiciado.setRg(rg);
    			
		indiciado = indiciadoBO.consultar(indiciado);
		String json = converterIndiciado.paraJson(indiciado);
		return json;
	}

    @GET
    @Path("indiciado/consultar/{id}")
	public String indiciadoConsultar(@PathParam("id") String id)
	{
 		Indiciado indiciado = indiciadoBO.consultar(id);
		String json = converterIndiciado.paraJson(indiciado);
		return json;
	}
    
    @POST
    @Path("indiciado/contar")
	public String indiciadoContar(InputStream ipFiltros)
	{
    	String json = InputStreamParaJson.converter(ipFiltros);
    	Map<String, String> filtros = gson.fromJson(json, Map.class);
		int count = indiciadoBO.contar(filtros);
		return Integer.toString(count);
	}
	
    @POST
    @Path("indiciado/pesquisarLazy/{primeiroRegistro}/{registrosPorPagina}")
	public String indiciadoPesquisarLazy(    		
			@PathParam("primeiroRegistro") int primeiroRegistro,
    		@PathParam("registrosPorPagina") int registrosPorPagina,
    		InputStream ipFiltros)
	{
    	String json = InputStreamParaJson.converter(ipFiltros);
    	Map<String, String> filtros = gson.fromJson(json, Map.class); 
    	Set<Indiciado> indiciados = indiciadoBO.pesquisarLazy(filtros, primeiroRegistro, registrosPorPagina);
    	json = converterIndiciado.paraJson(indiciados);
    	return json; 
	}

    @POST
    @Path("indiciado/pesquisarLazy/{primeiroRegistro}/{registrosPorPagina}/{campoOrdenacao}/{ordem}")
	public String indiciadoPesquisarLazy(    		
			@PathParam("primeiroRegistro") int primeiroRegistro,
    		@PathParam("registrosPorPagina") int registrosPorPagina,
    		@PathParam("campoOrdenacao") String campoOrdenacao,
    		@PathParam("ordem") int ordem,
    		InputStream ipFiltros)
	{
    	String json = InputStreamParaJson.converter(ipFiltros);
    	Map<String, String> filtros = gson.fromJson(json, Map.class); 
    	Set<Indiciado> indiciados = indiciadoBO.pesquisarLazy(filtros, primeiroRegistro, registrosPorPagina, campoOrdenacao, ordem);
    	json = converterIndiciado.paraJson(indiciados);
    	return json; 
	}

    
    @PUT
    @Path("indiciado/adicionar")
	public String indiciadoAdicionar(InputStream ipFiltros)
	{
    	String json = InputStreamParaJson.converter(ipFiltros);
    	Indiciado indiciado = converterIndiciado.paraObjeto(json, Indiciado.class);
    	indiciado = indiciadoBO.adicionar(indiciado);
    	json = converterIndiciado.paraJson(indiciado);
    	return json;
	}
    
    @POST
    @Path("indiciado/substituir")
	public void indiciadoSubstituir(InputStream ipFiltros)
	{
    	String json = InputStreamParaJson.converter(ipFiltros);
    	Indiciado indiciado = converterIndiciado.paraObjeto(json, Indiciado.class);
    	indiciadoBO.substituir(indiciado);
	}
    
    
}
