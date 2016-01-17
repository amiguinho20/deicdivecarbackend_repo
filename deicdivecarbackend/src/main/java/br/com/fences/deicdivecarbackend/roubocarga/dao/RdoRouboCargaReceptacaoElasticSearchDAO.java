package br.com.fences.deicdivecarbackend.roubocarga.dao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mongodb.BasicDBObject;

import br.com.fences.deicdivecarbackend.config.AppConfig;
import br.com.fences.deicdivecarbackend.config.Log;
import br.com.fences.fencesutils.conversor.converter.ColecaoJsonAdapter;
import br.com.fences.fencesutils.conversor.converter.Converter;
import br.com.fences.fencesutils.filtrocustom.ArvoreSimples;
import br.com.fences.fencesutils.filtrocustom.FiltroCondicao;
import br.com.fences.fencesutils.filtrocustom.TipoFiltro;
import br.com.fences.fencesutils.verificador.Verificador;
import br.com.fences.ocorrenciaentidade.composto.OcorrenciaResultadoComposto;
import br.com.fences.ocorrenciaentidade.ocorrencia.Ocorrencia;
import br.com.fences.ocorrenciaentidade.ocorrencia.auxiliar.Auxiliar;

@Log
@ApplicationScoped
public class RdoRouboCargaReceptacaoElasticSearchDAO {     

	private Logger logger =  LogManager.getLogger(RdoRouboCargaReceptacaoElasticSearchDAO.class);
	
	@Inject
	private Converter<Ocorrencia> converter;
	
	@Inject
	private AppConfig appConfig;
		
	private Gson gson = new GsonBuilder()
			.registerTypeHierarchyAdapter(Collection.class, new ColecaoJsonAdapter())
			.create();
	
	private static final String AGGREGATIONS = "aggregations"; 
	private static final String INDEX_TYPE = "/rdo_roubo_carga_recep06/bo/";
	private static final String BUCKETS = "buckets"; 
	private static final String KEY = "key"; 
	private static final String HITS = "hits";
	
	private String host;
	private String port;
	
	@PostConstruct
	private void init()
	{
		host = appConfig.getElasticSearchHost();
		port = appConfig.getElasticSearchPort();
	}
	
	
	
	/**
	 * Consulta pelo id (identificador unico), o "_id" e coloca associacoes
	 * @param id
	 */
	public Ocorrencia consultar(final String id)
	{
	    Ocorrencia ocorrencia = consultarSemAssociacao(id);
	    consultarPaiRef(ocorrencia);
	    consultarComplementares(ocorrencia);
	    return ocorrencia;
	}
	
	/**
	 * Consulta pelo id (identificador unico), o "_id"
	 * @param id
	 */
	private Ocorrencia consultarSemAssociacao(final String id)
	{
	    Ocorrencia ocorrencia = null;
		Client client = ClientBuilder.newClient();
		String servico = "http://" + host + ":"+ port + INDEX_TYPE + "{id}";
		WebTarget webTarget = client.target(servico);
		Response response = webTarget
				.resolveTemplate("id", id)
				.request(MediaType.APPLICATION_JSON)
				.get();
		String json = response.readEntity(String.class);
		
		JsonElement elemRaiz = gson.fromJson(json, JsonElement.class);
		JsonObject objRaiz = elemRaiz.getAsJsonObject();
		JsonElement elemSource = objRaiz.get("_source");
		if (elemSource != null)
		{
			json = elemSource.toString();
			ocorrencia = paraObjeto(json);
		}

	    return ocorrencia;
	}
	
	/**
	 * atualiza a lista por referencia
	 * @param ocorrencia
	 */
	private void consultarComplementares(Ocorrencia ocorrencia)
	{
		Auxiliar auxiliar = ocorrencia.getAuxiliar();
    	if (auxiliar != null && Verificador.isValorado(ocorrencia.getAuxiliar().getFilhos()))
    	{
        	Set<Ocorrencia> filhos = new LinkedHashSet<>();
        	for (Ocorrencia filho : auxiliar.getFilhos())
        	{
        		filho = consultarSemAssociacao(filho.getId());
        		filho.getAuxiliar().setPai(ocorrencia);
        		filhos.add(filho);
        	}
        	auxiliar.setFilhos(filhos); 
    	}
	}
	
	/**
	 * atualiza a lista por referencia
	 * @param ocorrencia
	 */
	private void consultarPaiRef(Ocorrencia ocorrencia)
	{
		Auxiliar auxiliar = ocorrencia.getAuxiliar();
    	if (auxiliar != null && ocorrencia.getAuxiliar().getPai() != null )
    	{
    		Ocorrencia pai = ocorrencia.getAuxiliar().getPai();
    		if (Verificador.isValorado(pai.getId()))
    		{
        		pai = consultarSemAssociacao(pai.getId());
        		auxiliar.setPai(pai);
    		}
    	}
	}

	public boolean isExisteNoBanco(Ocorrencia ocorrencia)
	{
		boolean existe = false;
//		if (Verificador.isValorado(ocorrencia.getId()))
//		{
//			if (consultarSemAssociacao(ocorrencia.getId()) != null)
//			{
//				existe = true;
//			}
//		}
//		else
//		{
//			BasicDBObject pesquisa = new BasicDBObject(); 
//			pesquisa.put("ID_DELEGACIA", ocorrencia.getIdDelegacia());
//			pesquisa.put("ANO_BO", ocorrencia.getAnoBo());
//			pesquisa.put("NUM_BO", ocorrencia.getNumBo());
//			Document documento = colecao.find(pesquisa).first();
//			if (documento != null)
//			{
//				existe = true;
//			}
//		}
		return existe;
	}
	
//	public Ocorrencia consultar(Ocorrencia ocorrencia)
//	{
//		BasicDBObject pesquisa = new BasicDBObject();
//		pesquisa.put("ID_DELEGACIA", ocorrencia.getIdDelegacia());
//		pesquisa.put("ANO_BO", ocorrencia.getAnoBo());
//		pesquisa.put("NUM_BO", ocorrencia.getNumBo());
//		Document documento = colecao.find(pesquisa).first();
//		Ocorrencia ocorrenciaConsultada = converter.paraObjeto(documento, Ocorrencia.class);
//		return ocorrenciaConsultada;
//	}
	
//	public Ocorrencia consultarPai(Ocorrencia filho)
//	{
//		Ocorrencia pai = null;
//		if (Verificador.isValorado(filho.getAnoReferenciaBo()))
//		{
//			BasicDBObject pesquisa = new BasicDBObject();
//			pesquisa.put("ID_DELEGACIA", filho.getDelegReferenciaBo());
//			pesquisa.put("ANO_BO", filho.getAnoReferenciaBo());
//			pesquisa.put("NUM_BO", filho.getNumReferenciaBo());
//			Document documento = colecao.find(pesquisa).first(); 
//			if (documento != null)
//			{
//				pai = converter.paraObjeto(documento, Ocorrencia.class);
//			}
//		}
//		return pai;
//	}
	
//	public Set<Ocorrencia> consultarFilhos(Ocorrencia pai)
//	{
//		Set<Ocorrencia> filhos = new LinkedHashSet<>();
//		
//		BasicDBObject pesquisa = new BasicDBObject();
//		pesquisa.put("DELEG_REFERENCIA_BO", pai.getIdDelegacia());
//		pesquisa.put("ANO_REFERENCIA_BO", pai.getAnoBo());
//		pesquisa.put("NUM_REFERENCIA_BO", pai.getNumBo());
//		
//	    MongoCursor<Document> cursor = colecao.find(pesquisa).iterator();
//	    
//	    try {
//	        while (cursor.hasNext()) {
//	        	Document documento = cursor.next();
//	        	Ocorrencia ocorrencia = converter.paraObjeto(documento, Ocorrencia.class);
//	        	consultarPaiRef(ocorrencia);
//	        	filhos.add(ocorrencia);
//	        }
//	    } finally {
//	        cursor.close();
//	    }
//	    return filhos;
//	}
	
	/**
	 * @param pesquisa
	 * @return count
	 */
	public int contarDinamico(List<FiltroCondicao> filtroCondicoes)
	{

	    JsonObject queryAtrib = montarPesquisa(filtroCondicoes);

	    JsonObject pesquisa = new JsonObject();
	    pesquisa.add("query", queryAtrib);
	    
		String json = pesquisa.toString();
		
		Client client = ClientBuilder.newClient();
		String servico = "http://" + host + ":"+ port + INDEX_TYPE + "_search";
		WebTarget webTarget = client.target(servico);
		Response response = webTarget
				.request(MediaType.APPLICATION_JSON)
				.post(Entity.json(json));
		json = response.readEntity(String.class);
		
		JsonObject elemRaiz = gson.fromJson(json, JsonObject.class);
		
		String qtdTotal = elemRaiz.get(HITS).getAsJsonObject().get("total").getAsString();
		
	    int count = 0;
	    if (Verificador.isInteiro(qtdTotal))
	    {
	    	count = Integer.parseInt(qtdTotal);
	    }
	    return count;
	}	
	
//	public int contar(final Map<String, String> filtros)
//	{
//		BasicDBObject dbFiltros = montarPesquisa(filtros);
//	    long countL = colecao.count(dbFiltros);
//	    int countI = (int) countL;
//	    
//	    return countI;
//	}	
	
	/**
	 * Pesquisa com <b>PAGINACAO</b>
	 * @param pesquisa
	 * @param primeiroRegistro
	 * @param registrosPorPagina
	 * @return List<Ocorrencia> paginado
	 */
//	@Log
//	public List<Ocorrencia> pesquisarLazy(final Map<String, String> filtros, final int primeiroRegistro, final int registrosPorPagina)
//	{
//		List<Ocorrencia> ocorrencias = new ArrayList<>();
//		
//		//-- filtros
////		BasicDBObject search = new BasicDBObject("$search", pesquisa);
////	    BasicDBObject text = new BasicDBObject("$text", search); 
//	    
//	    //-- ordenacoes
//	    BasicDBObject ordenacao = new BasicDBObject("DATAHORA_REGISTRO_BO", -1); 
//	    
//	   // db.rdo_roubo_carga_receptacao.find({"DATAHORA_REGISTRO_BO":{$gt:"20051229000000", $lt:"20051229999999"}},{"_id":0, "ANO_BO":1, "NUM_BO":1, "ID_DELEGACIA":1, "DATAHORA_REGISTRO_BO":1}).sort({"DATAHORA_REGISTRO_BO": -1}).count();
//	    BasicDBObject dbFiltros = montarPesquisa(filtros);
//	    MongoCursor<Document> cursor = colecao.find(dbFiltros).sort(ordenacao).skip(primeiroRegistro).limit(registrosPorPagina).iterator();
//	    
//	    try {
//	        while (cursor.hasNext()) {
//	        	Document documento = cursor.next();
//	        	Ocorrencia ocorrencia = converter.paraObjeto(documento, Ocorrencia.class);
//	        	consultarPaiRef(ocorrencia);
//	        	consultarComplementares(ocorrencia);
//	        	//pesquisarOcorrenciaComplementar(ocorrencia);
//	        	ocorrencias.add(ocorrencia);
//	        }
//	    } finally {
//	        cursor.close();
//	    }
//	    
//	    return ocorrencias;
//	}
	
	@Log
	public OcorrenciaResultadoComposto pesquisarDinamicoLazyComposto(List<FiltroCondicao> filtroCondicoes, int primeiroRegistro,
			int registrosPorPagina) {
		
		List<Ocorrencia> ocorrencias = new ArrayList<>();
		
		JsonObject order = new JsonObject();
		order.addProperty("order", "desc");
		
		JsonObject sortAtrib = new JsonObject();
		sortAtrib.add("DATAHORA_REGISTRO_BO", order);

	    JsonObject queryAtrib = montarPesquisa(filtroCondicoes);

	    JsonObject pesquisa = new JsonObject();
	    pesquisa.addProperty("size", registrosPorPagina);
	    pesquisa.addProperty("from", primeiroRegistro);
	    pesquisa.add("sort", sortAtrib);
	    pesquisa.add("query", queryAtrib);
	    
		String json = pesquisa.toString();
		
		Client client = ClientBuilder.newClient();
		String servico = "http://" + host + ":"+ port + INDEX_TYPE + "_search";
		WebTarget webTarget = client.target(servico);
		Response response = webTarget
				.request(MediaType.APPLICATION_JSON)
				.post(Entity.json(json));
		json = response.readEntity(String.class);
		
		JsonObject elemRaiz = gson.fromJson(json, JsonObject.class);
		
		String qtdTotal = elemRaiz.get(HITS).getAsJsonObject().get("total").getAsString();
		
		JsonArray hits = elemRaiz.get(HITS)
							.getAsJsonObject().get(HITS)
							.getAsJsonArray();
		for (Iterator<JsonElement> hit = hits.iterator(); hit.hasNext();)
		{
			JsonElement hitElem = hit.next();
			String source = hitElem.getAsJsonObject().get("_source").toString();
			Ocorrencia ocorrencia = paraObjeto(source);
			ocorrencias.add(ocorrencia);
			
		}
	    
		OcorrenciaResultadoComposto orc = new OcorrenciaResultadoComposto();
		orc.setQuantidadeTotal(Integer.parseInt(qtdTotal));
		orc.setOcorrencias(ocorrencias);
		
	    return orc;
	}
	
	
	@Log
	public List<Ocorrencia> pesquisarDinamicoLazy(List<FiltroCondicao> filtroCondicoes, int primeiroRegistro,
			int registrosPorPagina) {
		
		List<Ocorrencia> ocorrencias = new ArrayList<>();
		
		JsonObject order = new JsonObject();
		order.addProperty("order", "desc");
		
		JsonObject sortAtrib = new JsonObject();
		sortAtrib.add("DATAHORA_REGISTRO_BO", order);

	    JsonObject queryAtrib = montarPesquisa(filtroCondicoes);

	    JsonObject pesquisa = new JsonObject();
	    pesquisa.addProperty("size", registrosPorPagina);
	    pesquisa.addProperty("from", primeiroRegistro);
	    pesquisa.add("sort", sortAtrib);
	    pesquisa.add("query", queryAtrib);
	    
		String json = pesquisa.toString();
		
		Client client = ClientBuilder.newClient();
		String servico = "http://" + host + ":"+ port + INDEX_TYPE + "_search";
		WebTarget webTarget = client.target(servico);
		Response response = webTarget
				.request(MediaType.APPLICATION_JSON)
				.post(Entity.json(json));
		json = response.readEntity(String.class);
		
		JsonObject elemRaiz = gson.fromJson(json, JsonObject.class);
		
		//String qtdTotal = elemRaiz.get(HITS).getAsJsonObject().get("total").getAsString();
		
		JsonArray hits = elemRaiz.get(HITS)
							.getAsJsonObject().get(HITS)
							.getAsJsonArray();
		for (Iterator<JsonElement> hit = hits.iterator(); hit.hasNext();)
		{
			JsonElement hitElem = hit.next();
			String source = hitElem.getAsJsonObject().get("_source").toString();
			Ocorrencia ocorrencia = paraObjeto(source);
			ocorrencias.add(ocorrencia);
			
		}
	    
		//logger.info("qtdTotal: " + qtdTotal);
	    
	    return ocorrencias;
	}
	
//	public String pesquisarUltimaDataRegistroNaoComplementar()
//	{
//		String datahoraRegistroBo = null;
//		
//		BasicDBObject pesquisa = new BasicDBObject("ANO_REFERENCIA_BO", new BasicDBObject("$exists", false));
//		BasicDBObject projecao = new BasicDBObject("DATAHORA_REGISTRO_BO", 1).append("_id", 0);
//		BasicDBObject ordenacao = new BasicDBObject("DATAHORA_REGISTRO_BO", -1);
//		
//		MongoCursor<Document> cursor = colecao.find(pesquisa).projection(projecao).sort(ordenacao).limit(1).iterator();
//	
//	    try {
//	        if (cursor.hasNext()) {
//	        	Document documento = cursor.next();
//	        	datahoraRegistroBo = documento.getString("DATAHORA_REGISTRO_BO");
//	        }
//	    } finally {
//	        cursor.close();
//	    }
//	    return datahoraRegistroBo;
//	}
	
//	public String pesquisarPrimeiraDataRegistro()
//	{
//		return pesquisarPrimeiraDataRegistro(true);
//	}
//	
//	public String pesquisarUltimaDataRegistro()
//	{
//		return pesquisarPrimeiraDataRegistro(false);
//	}
//	
//	private String pesquisarPrimeiraDataRegistro(boolean primeiro)
//	{
//		String datahoraRegistroBo = null;
//		
//		int ordem = -1;
//		if (primeiro)
//		{
//			ordem = 1;
//		}
//		
//		BasicDBObject pesquisa = new BasicDBObject();
//		BasicDBObject projecao = new BasicDBObject("DATAHORA_REGISTRO_BO", 1).append("_id", 0);
//		BasicDBObject ordenacao = new BasicDBObject("DATAHORA_REGISTRO_BO", ordem);
//		
//		Document documento = colecao.find(pesquisa).projection(projecao).sort(ordenacao).first();
//	
//        if (documento != null) {
//        	datahoraRegistroBo = documento.getString("DATAHORA_REGISTRO_BO");
//        }
//
//	    return datahoraRegistroBo;
//	}
	
	/**
	 * Exemplo
	 * 	POST
	 * http://10.75.200.49:9200/rdo_roubo_carga_recep06/bo/_search?pretty
		{
			   "size": 0, 
			   "aggregations": {
			      "group_por_ano": {
			         "terms": {
			            "field": "ANO_BO",
			            "order" : { "_term" : "desc" }
			         }
			      }
			   }
			}
	 * 
	 */
	public List<String> listarAnos()
	{
		final String GROUP_POR_ANO = "group_por_ano"; //-- nome da agregacao
		
		
		JsonObject term = new JsonObject();
		term.addProperty("_term", "desc");
		
		JsonObject terms = new JsonObject();
		terms.addProperty("field", "ANO_BO");
		terms.add("order", term);
		
		JsonObject groupPorAno = new JsonObject();
		groupPorAno.add("terms", terms);
		
		JsonObject aggregations = new JsonObject();
		aggregations.add(GROUP_POR_ANO, groupPorAno);
		
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("size", 0);
		jsonObject.add(AGGREGATIONS, aggregations);
		
		String json = jsonObject.toString();
		
		Client client = ClientBuilder.newClient();
		String servico = "http://" + host + ":"+ port + INDEX_TYPE + "_search";
		WebTarget webTarget = client.target(servico);
		Response response = webTarget
				.request(MediaType.APPLICATION_JSON)
				.post(Entity.json(json));
		json = response.readEntity(String.class);
		
		List<String> anos = new ArrayList<>();
		
		JsonObject elemRaiz = gson.fromJson(json, JsonObject.class);
		JsonArray buckets = elemRaiz.get(AGGREGATIONS)
							.getAsJsonObject().get(GROUP_POR_ANO)
							.getAsJsonObject().get(BUCKETS)
							.getAsJsonArray();
		for (Iterator<JsonElement> bucketIter = buckets.iterator(); bucketIter.hasNext();)
		{
			JsonElement bucket = bucketIter.next();
			JsonObject bucketObj = bucket.getAsJsonObject();
			String ano = bucketObj.get(KEY).getAsString();
			anos.add(ano);
		}
		



		return anos;
	}

	/**
	{
			   "size": 0, 
			   "aggregations": {
			      "group_por_deleg": {
			         "terms": {
			            "field": "NOME_DELEGACIA",
						"size": 0, 
			            "order" : { "_term" : "asc" }
			         }
			      }
			   }
			}
			----
			
			{
			  "size": 0,
			  "aggs": {
			    "group_by_nome": {
			      "terms": {
			        "field": "NOME_DELEGACIA",
			        "size":0,
			        "order":{"_term":"asc"}
			      },
			      "aggs": {
			        "group_by_id": {
			          "terms": {
			            "field": "ID_DELEGACIA"
			          }
			        }
			      }
			    }
			  }
			}			
	 */
	public Map<String, String> listarDelegacias()
	{
		Map<String, String> delegacias = new LinkedHashMap<>();

		final String GROUP_POR_NOME = "group_por_nome"; //-- nome da agregacao
		final String GROUP_POR_ID = "group_por_id"; //-- nome da agregacao
		
		
		//---
		JsonObject idDelegacia = new JsonObject();
		idDelegacia.addProperty("field", "ID_DELEGACIA");
		
		JsonObject termsId = new JsonObject();
		termsId.add("terms", idDelegacia);
		
		JsonObject groupPorId = new JsonObject();
		groupPorId.add(GROUP_POR_ID, termsId);
		
		JsonObject ordem = new JsonObject();
		ordem.addProperty("_term", "asc");
		
		JsonObject terms = new JsonObject();
		terms.addProperty("field", "NOME_DELEGACIA");
		terms.add("order", ordem);
		terms.addProperty("size", 0);
		
		JsonObject groupPorNome = new JsonObject();
		groupPorNome.add("terms", terms);
		groupPorNome.add(AGGREGATIONS, groupPorId);
		
		JsonObject aggNome = new JsonObject();
		aggNome.add(GROUP_POR_NOME, groupPorNome);
		
		JsonObject comandoAgregacao = new JsonObject();
		comandoAgregacao.addProperty("size", 0);
		comandoAgregacao.add(AGGREGATIONS, aggNome);
		
		
		String json = comandoAgregacao.toString();
		
		Client client = ClientBuilder.newClient();
		String servico = "http://" + host + ":"+ port + INDEX_TYPE + "_search";
		WebTarget webTarget = client.target(servico);
		Response response = webTarget
				.request(MediaType.APPLICATION_JSON)
				.post(Entity.json(json));
		json = response.readEntity(String.class);
		
		
		
		JsonObject elemRaiz = gson.fromJson(json, JsonObject.class);
		JsonArray buckets = elemRaiz.get(AGGREGATIONS)
							.getAsJsonObject().get(GROUP_POR_NOME)
							.getAsJsonObject().get(BUCKETS)
							.getAsJsonArray();
		for (Iterator<JsonElement> bucketIter = buckets.iterator(); bucketIter.hasNext();)
		{
			JsonElement bucket = bucketIter.next();
			JsonObject bucketObj = bucket.getAsJsonObject();
			String nomeDelegacia = bucketObj.get(KEY).getAsString();
			
			JsonArray bucketsId = bucketObj.get(GROUP_POR_ID)
					.getAsJsonObject().get(BUCKETS)
					.getAsJsonArray();
			
			String codDelegacia = bucketsId.get(0).getAsJsonObject().get(KEY).getAsString();
			delegacias.put(codDelegacia, nomeDelegacia);
			
		}
		
		return delegacias;
	}

/*
	public Map<String, String> listarTipoObjetos()
	{
		Map<String, String> tipos = new LinkedHashMap<>();
		
		//MongoCursor<String> cursor = colecao.distinct("ANO_BO", String.class).iterator();
		
//		db.rdo_roubo_carga_recep06.aggregate(
//				[
//					{"$match":{"OBJETO":{"$exists":true}}},
//					{"$project":{
//						"_id":0, 
//						"OBJETO.ID_TIPO_OBJETO":1, 
//						"OBJETO.DESCR_TIPO_OBJETO":1,
//						"OBJETO.ID_SUBTIPO_OBJETO":1, 
//						"OBJETO.DESCR_SUBTIPO_OBJETO":1
//						}
//					},
//					{"$unwind":"$OBJETO"},
//					{"$group": {
//						"_id": {
//							"ID_TIPO":"$OBJETO.ID_TIPO_OBJETO",
//							"ID_SUBTIPO":"$OBJETO.ID_SUBTIPO_OBJETO"
//							},
//						"DESCR_TIPO":{"$first":"$OBJETO.DESCR_TIPO_OBJETO"},
//						"DESCR_SUBTIPO":{"$first":"$OBJETO.DESCR_SUBTIPO_OBJETO"}
//						}
//					},
//					{"$sort": {"DESCR_TIPO":1,"DESCR_SUBTIPO":1}}
//				]
//			)
		
		//-- match: filtro, condicao de busca
		BasicDBObject match = new BasicDBObject("$match", new BasicDBObject("OBJETO", new BasicDBObject("$exists", true)));
		
		//-- project: projecao, informacao de retorno
		BasicDBObject projectAtributos = new BasicDBObject("_id", 0);
		projectAtributos.append("OBJETO.ID_TIPO_OBJETO", 1);
		projectAtributos.append("OBJETO.DESCR_TIPO_OBJETO", 1);
		projectAtributos.append("OBJETO.ID_SUBTIPO_OBJETO", 1);
		projectAtributos.append("OBJETO.DESCR_SUBTIPO_OBJETO", 1);
		
		//-- project: comando
		BasicDBObject project= new BasicDBObject("$project", projectAtributos);
		
		//-- unwind: "quebra" o array, desnormalizando-o
		BasicDBObject unwind = new BasicDBObject("$unwind", "$OBJETO");
		
		//-- groupId: chave do agrupamento
		BasicDBObject groupId = new BasicDBObject();
		groupId.append("ID_TIPO","$OBJETO.ID_TIPO_OBJETO");
		groupId.append("ID_SUBTIPO","$OBJETO.ID_SUBTIPO_OBJETO");
		
		//-- groupAtributos: chave mais informacao de agrupamento
		BasicDBObject groupAtributos = new BasicDBObject("_id", groupId);
		groupAtributos.append("DESCR_TIPO", new BasicDBObject("$first", "$OBJETO.DESCR_TIPO_OBJETO"));
		groupAtributos.append("DESCR_SUBTIPO", new BasicDBObject("$first", "$OBJETO.DESCR_SUBTIPO_OBJETO"));

		//-- group: comando de agrupamento
		BasicDBObject group = new BasicDBObject("$group", groupAtributos);  
		
		//-- sortAtributos: atributos para ordenacao
		BasicDBObject sortAtributos = new BasicDBObject();
		sortAtributos.append("DESCR_TIPO", 1);
		sortAtributos.append("DESCR_SUBTIPO", 1);
		
		//-- sort: comando de ordenacao
		BasicDBObject sort = new BasicDBObject("$sort", sortAtributos);
		
		//-- pipeline: instrucoes em ordem de execucao
		List<BasicDBObject> pipeline = Arrays.asList(match, project, unwind, group, sort);
		
		MongoCursor<Document> cursor = colecao.aggregate(pipeline).iterator();
		
	    try {
	        while (cursor.hasNext()) {
	        	Document documento = cursor.next();
	        	Document idDoc = (Document) documento.get("_id");
	        	String chave = idDoc.getString("ID_TIPO") + "|" + idDoc.getString("ID_SUBTIPO");
	        	String valor = documento.getString("DESCR_TIPO") + " > " + documento.getString("DESCR_SUBTIPO");
	        	tipos.put(chave, valor);
	        }
	    } finally {
	        cursor.close();
	    }
	    
	    //-- ordernar o map por valor
	    tipos = ordenarPorValor(tipos);
	    
		return tipos;
	}
*/	
//	public Map<String, String> listarTipoPessoas()
//	{
//		Map<String, String> tipos = new LinkedHashMap<>();
//		
//		//-- match: filtro, condicao de busca
//		BasicDBObject match = new BasicDBObject("$match", new BasicDBObject("PESSOA.ID_TIPO_PESSOA", new BasicDBObject("$exists", true)));
//		
//		//-- project: projecao, informacao de retorno
//		BasicDBObject projectAtributos = new BasicDBObject("_id", 0);
//		projectAtributos.append("PESSOA.ID_TIPO_PESSOA", 1);
//		projectAtributos.append("PESSOA.DESCR_TIPO_PESSOA", 1);
//
//		//-- project: comando
//		BasicDBObject project= new BasicDBObject("$project", projectAtributos);
//		
//		//-- unwind: "quebra" o array, desnormalizando-o
//		BasicDBObject unwind = new BasicDBObject("$unwind", "$PESSOA");
//		
//		//-- groupId: chave do agrupamento
//		BasicDBObject groupId = new BasicDBObject();
//		groupId.append("ID_TIPO_PESSOA","$PESSOA.ID_TIPO_PESSOA");
//		groupId.append("DESCR_TIPO_PESSOA","$PESSOA.DESCR_TIPO_PESSOA");
//		
//		//-- groupAtributos: chave mais informacao de agrupamento
//		BasicDBObject groupAtributos = new BasicDBObject("_id", groupId);
////		groupAtributos.append("DESCR_TIPO", new BasicDBObject("$first", "$OBJETO.DESCR_TIPO_OBJETO"));
////		groupAtributos.append("DESCR_SUBTIPO", new BasicDBObject("$first", "$OBJETO.DESCR_SUBTIPO_OBJETO"));
//
//		//-- group: comando de agrupamento
//		BasicDBObject group = new BasicDBObject("$group", groupAtributos);  
//		
//		//-- sortAtributos: atributos para ordenacao
//		BasicDBObject sortAtributos = new BasicDBObject();
//		sortAtributos.append("_id.DESCR_TIPO_PESSOA", 1);
//		
//		//-- sort: comando de ordenacao
//		BasicDBObject sort = new BasicDBObject("$sort", sortAtributos);
//		
//		//-- pipeline: instrucoes em ordem de execucao
//		List<BasicDBObject> pipeline = Arrays.asList(match, project, unwind, group, sort);
//		
//		MongoCursor<Document> cursor = colecao.aggregate(pipeline).iterator();
//		
//	    try {
//	        while (cursor.hasNext()) {
//	        	Document documento = cursor.next();
//	        	Document idDoc = (Document) documento.get("_id");
//	        	String chave = idDoc.getString("ID_TIPO_PESSOA");
//	        	String valor = idDoc.getString("DESCR_TIPO_PESSOA");
//	        	tipos.put(chave, valor);
//	        }
//	    } finally {
//	        cursor.close();
//	    }
//	    
//		return tipos;
//	}


/*	
	public Map<String, String> listarNaturezas()
	{
		Map<String, String> naturezas = new LinkedHashMap<>();
		
//		db.rdo_roubo_carga_recep06.aggregate(
//				[
//					{"$project":{
//						"_id":0, 
//						"NATUREZA.ID_OCORRENCIA":1, 
//						"NATUREZA.DESCR_OCORRENCIA":1,
//						"NATUREZA.ID_ESPECIE":1, 
//						"NATUREZA.DESCR_ESPECIE":1,
//						"NATUREZA.ID_SUBESPECIE":1, 
//						"NATUREZA.DESCR_SUBESPECIE":1,
//						"NATUREZA.ID_NATUREZA":1, 
//						"NATUREZA.RUBRICA_NATUREZA":1,
//						"NATUREZA.ID_CONDUTA":1, 
//						"NATUREZA.DESCR_CONDUTA":1
//						}
//					},
//					{"$unwind":"$NATUREZA"},
//					{"$group": {
//						"_id": {
//							"ID_OCORRENCIA":"$NATUREZA.ID_OCORRENCIA",
//							"ID_ESPECIE":"$NATUREZA.ID_ESPECIE",
//							"ID_SUBESPECIE":"$NATUREZA.ID_SUBESPECIE",
//							"ID_NATUREZA":"$NATUREZA.ID_NATUREZA",
//							"ID_CONDUTA":"$NATUREZA.ID_CONDUTA"
//							},
//						"DESCR_OCORRENCIA":{"$first":"$NATUREZA.DESCR_OCORRENCIA"},
//						"DESCR_ESPECIE":{"$first":"$NATUREZA.DESCR_ESPECIE"},
//						"DESCR_SUBESPECIE":{"$first":"$NATUREZA.DESCR_SUBESPECIE"},
//						"RUBRICA_NATUREZA":{"$first":"$NATUREZA.RUBRICA_NATUREZA"},
//						"DESCR_CONDUTA":{"$first":"$NATUREZA.DESCR_CONDUTA"}
//						}
//					},
//					{"$sort": {
//						"_id.ID_OCORRENCIA":1, 
//						"_id.ID_ESPECIE":1,
//						"_id.ID_SUBESPECIE":1,
//						"_id.ID_NATUREZA":1,
//						"_id.ID_CONDUTA":1
//						}
//					}
//				]
//			)

		
//		//-- match: filtro, condicao de busca
//		BasicDBObject match = new BasicDBObject("$match", new BasicDBObject("OBJETO", new BasicDBObject("$exists", true)));
		
		//-- project: projecao, informacao de retorno
		BasicDBObject projectAtributos = new BasicDBObject("_id", 0);
		projectAtributos.append("NATUREZA.ID_OCORRENCIA", 1);
		projectAtributos.append("NATUREZA.DESCR_OCORRENCIA", 1);
		projectAtributos.append("NATUREZA.ID_ESPECIE", 1);
		projectAtributos.append("NATUREZA.DESCR_ESPECIE", 1);
		projectAtributos.append("NATUREZA.ID_SUBESPECIE", 1);
		projectAtributos.append("NATUREZA.DESCR_SUBESPECIE", 1);
		projectAtributos.append("NATUREZA.ID_NATUREZA", 1);
		projectAtributos.append("NATUREZA.RUBRICA_NATUREZA", 1);
		projectAtributos.append("NATUREZA.ID_CONDUTA", 1);
		projectAtributos.append("NATUREZA.DESCR_CONDUTA", 1);
		
		//-- project: comando
		BasicDBObject project= new BasicDBObject("$project", projectAtributos);
		
		//-- unwind: "quebra" o array, desnormalizando-o
		BasicDBObject unwind = new BasicDBObject("$unwind", "$NATUREZA");
		
		//-- groupId: chave do agrupamento
		BasicDBObject groupId = new BasicDBObject();
		groupId.append("ID_OCORRENCIA","$NATUREZA.ID_OCORRENCIA");
		groupId.append("ID_ESPECIE","$NATUREZA.ID_ESPECIE");
		groupId.append("ID_SUBESPECIE","$NATUREZA.ID_SUBESPECIE");
		groupId.append("ID_NATUREZA","$NATUREZA.ID_NATUREZA");
		groupId.append("ID_CONDUTA","$NATUREZA.ID_CONDUTA");
		
		//-- groupAtributos: informacao de agrupamento
		BasicDBObject groupAtributos = new BasicDBObject("_id", groupId);
		groupAtributos.append("DESCR_OCORRENCIA", new BasicDBObject("$first", "$NATUREZA.DESCR_OCORRENCIA"));
		groupAtributos.append("DESCR_ESPECIE", new BasicDBObject("$first", "$NATUREZA.DESCR_ESPECIE"));
		groupAtributos.append("DESCR_SUBESPECIE", new BasicDBObject("$first", "$NATUREZA.DESCR_SUBESPECIE"));
		groupAtributos.append("RUBRICA_NATUREZA", new BasicDBObject("$first", "$NATUREZA.RUBRICA_NATUREZA"));
		groupAtributos.append("DESCR_CONDUTA", new BasicDBObject("$first", "$NATUREZA.DESCR_CONDUTA"));

		//-- group: comando de agrupamento
		BasicDBObject group = new BasicDBObject("$group", groupAtributos);  
		
		//-- sortAtributos: atributos para ordenacao
		BasicDBObject sortAtributos = new BasicDBObject();
		sortAtributos.append("_id.ID_OCORRENCIA", 1);
		sortAtributos.append("_id.ID_ESPECIE", 1);
		sortAtributos.append("_id.ID_SUBESPECIE", 1);
		sortAtributos.append("_id.ID_NATUREZA", 1);
		sortAtributos.append("_id.ID_CONDUTA", 1);

		//-- sort: comando de ordenacao
		BasicDBObject sort = new BasicDBObject("$sort", sortAtributos);
		
		//-- pipeline: instrucoes em ordem de execucao
		List<BasicDBObject> pipeline = Arrays.asList(project, unwind, group, sort);
		
		MongoCursor<Document> cursor = colecao.aggregate(pipeline).iterator();
		
	    try {
	        while (cursor.hasNext()) {
	        	Document doc = cursor.next();
	        	Document idDoc = (Document) doc.get("_id");

	        	StringBuffer chave = new StringBuffer();
	        	chave.append("ID_OCORRENCIA:");
	        	chave.append(idDoc.getString("ID_OCORRENCIA"));

	        	chave.append("|");
	        	chave.append("ID_ESPECIE:");
	        	chave.append(idDoc.getString("ID_ESPECIE"));
	        	
	        	chave.append("|");
	        	chave.append("ID_SUBESPECIE:");
	        	chave.append(idDoc.getString("ID_SUBESPECIE"));

	        	chave.append("|");
	        	chave.append("ID_NATUREZA:");
	        	chave.append(idDoc.getString("ID_NATUREZA"));
	        	
	        	boolean contemConduta = false;
	        	if (idDoc.containsKey("ID_CONDUTA"))
	        	{
		        	chave.append("|");
		        	chave.append("ID_CONDUTA:");
		        	chave.append(idDoc.getString("ID_CONDUTA"));
		        	contemConduta = true;
	        	}
	        		
	        	
	        	String valor =	doc.getString("DESCR_OCORRENCIA") + " > " + 
	        					doc.getString("DESCR_ESPECIE") + " > " + 
	        					doc.getString("DESCR_SUBESPECIE") + " > " +
	        					doc.getString("RUBRICA_NATUREZA");
	        	if (contemConduta)
	        	{
	        		valor += " > " + doc.getString("DESCR_CONDUTA");
	        	}
	        	
	        	naturezas.put(chave.toString(), valor);
	        }
	    } finally {
	        cursor.close();
	    }
	    
	    //-- ordernar o map por valor
	    //tipos = ordenarPorValor(tipos);
	    
		return naturezas;
	}
*/	
//	public Map<String, String> listarNaturezasComDesdobramentoCircunstancia()
//	{
//		Map<String, String> naturezas = new LinkedHashMap<>();
//		
///*
//
//db.rdo_roubo_carga_recep06.aggregate(
//		[
//			{"$project":{
//				"_id":0, 
//				"NATUREZA.ID_OCORRENCIA":1, 
//				"NATUREZA.DESCR_OCORRENCIA":1,
//				"NATUREZA.ID_ESPECIE":1, 
//				"NATUREZA.DESCR_ESPECIE":1,
//				"NATUREZA.ID_SUBESPECIE":1, 
//				"NATUREZA.DESCR_SUBESPECIE":1,
//				"NATUREZA.ID_NATUREZA":1, 
//				"NATUREZA.RUBRICA_NATUREZA":1,
//				"NATUREZA.ID_CONDUTA":1, 
//				"NATUREZA.DESCR_CONDUTA":1,
//				"NATUREZA.DESDOBRAMENTO.ID_DESDOBRAMENTO":1,
//				"NATUREZA.DESDOBRAMENTO.DESCR_DESDOBRAMENTO":1,
//				"NATUREZA.CIRCUNSTANCIA.ID_CIRCUNSTANCIA":1,
//				"NATUREZA.CIRCUNSTANCIA.DESCR_CIRCUNSTANCIA":1
//				}
//			},
//			{"$unwind":"$NATUREZA"},
//			{"$project":{
//				"_id":0, 
//				"ID_OCORRENCIA":"$NATUREZA.ID_OCORRENCIA", 
//				"DESCR_OCORRENCIA":"$NATUREZA.DESCR_OCORRENCIA",
//				"ID_ESPECIE": "$NATUREZA.ID_ESPECIE", 
//				"DESCR_ESPECIE": "$NATUREZA.DESCR_ESPECIE",
//				"ID_SUBESPECIE": "$NATUREZA.ID_SUBESPECIE", 
//				"DESCR_SUBESPECIE":"$NATUREZA.DESCR_SUBESPECIE",
//				"ID_NATUREZA": "$NATUREZA.ID_NATUREZA", 
//				"RUBRICA_NATUREZA": "$NATUREZA.RUBRICA_NATUREZA",
//				"ID_CONDUTA": "$NATUREZA.ID_CONDUTA", 
//				"DESCR_CONDUTA": "$NATUREZA.DESCR_CONDUTA",
//				"DESDOBRAMENTO": {"$ifNull": ["$NATUREZA.DESDOBRAMENTO", [{"ID_DESDOBRAMENTO":null}] ]},
//				"CIRCUNSTANCIA": {"$ifNull": ["$NATUREZA.CIRCUNSTANCIA", [{"ID_CIRCUNSTANCIA":null}] ]}
//				}
//			},
//			{"$unwind":"$DESDOBRAMENTO"},
//			{"$unwind":"$CIRCUNSTANCIA"},
//			{"$project":{
//				"_id":0, 
//				"ID_OCORRENCIA":1, 
//				"DESCR_OCORRENCIA":1,
//				"ID_ESPECIE": 1, 
//				"DESCR_ESPECIE": 1,
//				"ID_SUBESPECIE": 1, 
//				"DESCR_SUBESPECIE":1,
//				"ID_NATUREZA": 1, 
//				"RUBRICA_NATUREZA": 1,
//				"ID_CONDUTA": 1, 
//				"DESCR_CONDUTA": 1,
//				"ID_DESDOBRAMENTO":"$DESDOBRAMENTO.ID_DESDOBRAMENTO",
//				"DESCR_DESDOBRAMENTO":"$DESDOBRAMENTO.DESCR_DESDOBRAMENTO",
//				"ID_CIRCUNSTANCIA": "$CIRCUNSTANCIA.ID_CIRCUNSTANCIA",
//				"DESCR_CIRCUNSTANCIA": "$CIRCUNSTANCIA.DESCR_CIRCUNSTANCIA"
//				}
//			},
//			{"$group": {
//				"_id": {
//					"ID_OCORRENCIA":"$ID_OCORRENCIA",
//					"ID_ESPECIE":"$ID_ESPECIE",
//					"ID_SUBESPECIE":"$ID_SUBESPECIE",
//					"ID_NATUREZA":"$ID_NATUREZA",
//					"ID_CONDUTA":"$ID_CONDUTA",
//					"ID_DESDOBRAMENTO":"$ID_DESDOBRAMENTO",
//					"ID_CIRCUNSTANCIA":"$ID_CIRCUNSTANCIA"
//					},
//				"DESCR_OCORRENCIA":{"$first":"$DESCR_OCORRENCIA"},
//				"DESCR_ESPECIE":{"$first":"$DESCR_ESPECIE"},
//				"DESCR_SUBESPECIE":{"$first":"$DESCR_SUBESPECIE"},
//				"RUBRICA_NATUREZA":{"$first":"$RUBRICA_NATUREZA"},
//				"DESCR_CONDUTA":{"$first":"$DESCR_CONDUTA"},
//				"DESCR_DESDOBRAMENTO":{"$first":"$DESCR_DESDOBRAMENTO"},
//				"DESCR_CIRCUNSTANCIA":{"$first":"$DESCR_CIRCUNSTANCIA"}
//				}
//			},
//			{"$sort": {
//				"_id.ID_OCORRENCIA":1, 
//				"_id.ID_ESPECIE":1,
//				"_id.ID_SUBESPECIE":1,
//				"_id.ID_NATUREZA":1,
//				"_id.ID_CONDUTA":1,
//				"_id.ID_DESDOBRAMENTO":1,
//				"_id.ID_CIRCUNSTANCIA":1
//				}
//			}
//		]
//	)		
// */
//		
//		//-- project: projecao, informacao de retorno
//		BasicDBObject projectAttribNatureza = new BasicDBObject("_id", 0);
//		projectAttribNatureza.append("NATUREZA.ID_OCORRENCIA", 1);
//		projectAttribNatureza.append("NATUREZA.DESCR_OCORRENCIA", 1);
//		projectAttribNatureza.append("NATUREZA.ID_ESPECIE", 1);
//		projectAttribNatureza.append("NATUREZA.DESCR_ESPECIE", 1);
//		projectAttribNatureza.append("NATUREZA.ID_SUBESPECIE", 1);
//		projectAttribNatureza.append("NATUREZA.DESCR_SUBESPECIE", 1);
//		projectAttribNatureza.append("NATUREZA.ID_NATUREZA", 1);
//		projectAttribNatureza.append("NATUREZA.RUBRICA_NATUREZA", 1);
//		projectAttribNatureza.append("NATUREZA.ID_CONDUTA", 1);
//		projectAttribNatureza.append("NATUREZA.DESCR_CONDUTA", 1);
//		projectAttribNatureza.append("NATUREZA.DESDOBRAMENTO.ID_DESDOBRAMENTO", 1);
//		projectAttribNatureza.append("NATUREZA.DESDOBRAMENTO.DESCR_DESDOBRAMENTO", 1);
//		projectAttribNatureza.append("NATUREZA.CIRCUNSTANCIA.ID_CIRCUNSTANCIA", 1);
//		projectAttribNatureza.append("NATUREZA.CIRCUNSTANCIA.DESCR_CIRCUNSTANCIA", 1);
//		
//		//-- project: comando
//		BasicDBObject projectNatureza= new BasicDBObject("$project", projectAttribNatureza);
//		
//		//-- unwind: "quebra" o array, desnormalizando-o
//		BasicDBObject unwindNatureza = new BasicDBObject("$unwind", "$NATUREZA");
//		
//		//-- project: projecao, informacao de retorno
//		BasicDBObject projectAttribDesdobrConduta = new BasicDBObject("_id", 0);
//		projectAttribDesdobrConduta.append("ID_OCORRENCIA", "$NATUREZA.ID_OCORRENCIA");
//		projectAttribDesdobrConduta.append("DESCR_OCORRENCIA", "$NATUREZA.DESCR_OCORRENCIA");
//		projectAttribDesdobrConduta.append("ID_ESPECIE", "$NATUREZA.ID_ESPECIE");
//		projectAttribDesdobrConduta.append("DESCR_ESPECIE", "$NATUREZA.DESCR_ESPECIE");
//		projectAttribDesdobrConduta.append("ID_SUBESPECIE", "$NATUREZA.ID_SUBESPECIE");
//		projectAttribDesdobrConduta.append("DESCR_SUBESPECIE", "$NATUREZA.DESCR_SUBESPECIE");
//		projectAttribDesdobrConduta.append("ID_NATUREZA", "$NATUREZA.ID_NATUREZA");
//		projectAttribDesdobrConduta.append("RUBRICA_NATUREZA", "$NATUREZA.RUBRICA_NATUREZA");
//		projectAttribDesdobrConduta.append("ID_CONDUTA", "$NATUREZA.ID_CONDUTA");
//		projectAttribDesdobrConduta.append("DESCR_CONDUTA", "$NATUREZA.DESCR_CONDUTA");
//		projectAttribDesdobrConduta.append("DESDOBRAMENTO", new BasicDBObject("$ifNull",
//				Arrays.asList("$NATUREZA.DESDOBRAMENTO", Arrays.asList(new BasicDBObject("ID_DESDOBRAMENTO", null)))));
//		projectAttribDesdobrConduta.append("CIRCUNSTANCIA", new BasicDBObject("$ifNull",
//				Arrays.asList("$NATUREZA.CIRCUNSTANCIA", Arrays.asList(new BasicDBObject("ID_CIRCUNSTANCIA", null)))));		
//
//		//-- project: comando
//		BasicDBObject projectDesdobrConduta = new BasicDBObject("$project", projectAttribDesdobrConduta);
//
//		BasicDBObject unwindDesdobramento = new BasicDBObject("$unwind", "$DESDOBRAMENTO");
//		BasicDBObject unwindCircunstancia = new BasicDBObject("$unwind", "$CIRCUNSTANCIA");
//
//		BasicDBObject projectAttribTabular = new BasicDBObject("_id", 0);
//		projectAttribTabular.append("ID_OCORRENCIA", 1);
//		projectAttribTabular.append("DESCR_OCORRENCIA", 1);
//		projectAttribTabular.append("ID_ESPECIE", 1);
//		projectAttribTabular.append("DESCR_ESPECIE", 1);
//		projectAttribTabular.append("ID_SUBESPECIE", 1);
//		projectAttribTabular.append("DESCR_SUBESPECIE", 1);
//		projectAttribTabular.append("ID_NATUREZA", 1);
//		projectAttribTabular.append("RUBRICA_NATUREZA", 1);
//		projectAttribTabular.append("ID_CONDUTA", 1);
//		projectAttribTabular.append("DESCR_CONDUTA", 1);
//		projectAttribTabular.append("ID_DESDOBRAMENTO", "$DESDOBRAMENTO.ID_DESDOBRAMENTO");
//		projectAttribTabular.append("DESCR_DESDOBRAMENTO", "$DESDOBRAMENTO.DESCR_DESDOBRAMENTO");
//		projectAttribTabular.append("ID_CIRCUNSTANCIA", "$CIRCUNSTANCIA.ID_CIRCUNSTANCIA");
//		projectAttribTabular.append("DESCR_CIRCUNSTANCIA", "$CIRCUNSTANCIA.DESCR_CIRCUNSTANCIA");
//		
//		//-- project: comando
//		BasicDBObject projectTabular= new BasicDBObject("$project", projectAttribTabular);
//
//		//-- groupId: chave do agrupamento
//		BasicDBObject groupId = new BasicDBObject();
//		groupId.append("ID_OCORRENCIA","$ID_OCORRENCIA");
//		groupId.append("ID_ESPECIE","$ID_ESPECIE");
//		groupId.append("ID_SUBESPECIE","$ID_SUBESPECIE");
//		groupId.append("ID_NATUREZA","$ID_NATUREZA");
//		groupId.append("ID_CONDUTA","$ID_CONDUTA");
//		groupId.append("ID_DESDOBRAMENTO","$ID_DESDOBRAMENTO");
//		groupId.append("ID_CIRCUNSTANCIA","$ID_CIRCUNSTANCIA");
//		
//		//-- groupAtributos: informacao de agrupamento
//		BasicDBObject groupAtributos = new BasicDBObject("_id", groupId);
//		groupAtributos.append("DESCR_OCORRENCIA", new BasicDBObject("$first", "$DESCR_OCORRENCIA"));
//		groupAtributos.append("DESCR_ESPECIE", new BasicDBObject("$first", "$DESCR_ESPECIE"));
//		groupAtributos.append("DESCR_SUBESPECIE", new BasicDBObject("$first", "$DESCR_SUBESPECIE"));
//		groupAtributos.append("RUBRICA_NATUREZA", new BasicDBObject("$first", "$RUBRICA_NATUREZA"));
//		groupAtributos.append("DESCR_CONDUTA", new BasicDBObject("$first", "$DESCR_CONDUTA"));
//		groupAtributos.append("DESCR_DESDOBRAMENTO", new BasicDBObject("$first", "$DESCR_DESDOBRAMENTO"));
//		groupAtributos.append("DESCR_CIRCUNSTANCIA", new BasicDBObject("$first", "$DESCR_CIRCUNSTANCIA"));
//
//		//-- group: comando de agrupamento
//		BasicDBObject group = new BasicDBObject("$group", groupAtributos);  
//		
//		//-- sortAtributos: atributos para ordenacao
//		BasicDBObject sortAtributos = new BasicDBObject();
//		sortAtributos.append("_id.ID_OCORRENCIA", 1);
//		sortAtributos.append("_id.ID_ESPECIE", 1);
//		sortAtributos.append("_id.ID_SUBESPECIE", 1);
//		sortAtributos.append("_id.ID_NATUREZA", 1);
//		sortAtributos.append("_id.ID_CONDUTA", 1);
//		sortAtributos.append("_id.ID_DESDOBRAMENTO", 1);
//		sortAtributos.append("_id.ID_CIRCUNSTANCIA", 1);
//
//		//-- sort: comando de ordenacao
//		BasicDBObject sort = new BasicDBObject("$sort", sortAtributos);
//		
//		//-- pipeline: instrucoes em ordem de execucao
//		List<BasicDBObject> pipeline = Arrays.asList(projectNatureza, unwindNatureza, projectDesdobrConduta,
//				unwindDesdobramento, unwindCircunstancia, projectTabular, group, sort);
//		
//		MongoCursor<Document> cursor = colecao.aggregate(pipeline).iterator();
//		
//	    try {
//	        while (cursor.hasNext()) {
//	        	Document doc = cursor.next();
//	        	Document idDoc = (Document) doc.get("_id");
//
//	        	StringBuffer chave = new StringBuffer();
//	        	StringBuffer valor = new StringBuffer();
//	        	
//	        	chave.append("ID_OCORRENCIA:");
//	        	chave.append(idDoc.getString("ID_OCORRENCIA"));
//	        	valor.append(doc.getString("DESCR_OCORRENCIA"));
//
//	        	chave.append("|");
//	        	chave.append("ID_ESPECIE:");
//	        	chave.append(idDoc.getString("ID_ESPECIE"));
//	        	valor.append(" > ");
//	        	valor.append(doc.getString("DESCR_ESPECIE"));
//	        	
//	        	chave.append("|");
//	        	chave.append("ID_SUBESPECIE:");
//	        	chave.append(idDoc.getString("ID_SUBESPECIE"));
//	        	valor.append(" > ");
//	        	valor.append(doc.getString("DESCR_SUBESPECIE"));
//
//	        	chave.append("|");
//	        	chave.append("ID_NATUREZA:");
//	        	chave.append(idDoc.getString("ID_NATUREZA"));
//	        	valor.append(" > ");
//	        	valor.append(doc.getString("RUBRICA_NATUREZA"));
//	        	
//	        	if (idDoc.containsKey("ID_CONDUTA") && Verificador.isValorado(idDoc.getString("ID_CONDUTA")))
//	        	{
//		        	chave.append("|");
//		        	chave.append("ID_CONDUTA:");
//		        	chave.append(idDoc.getString("ID_CONDUTA"));
//		        	valor.append(" > ");
//		        	valor.append(doc.getString("DESCR_CONDUTA"));
//	        	}
//	        	
//	        	if (idDoc.containsKey("ID_DESDOBRAMENTO") && Verificador.isValorado(idDoc.getString("ID_DESDOBRAMENTO")))
//	        	{
//		        	chave.append("|");
//		        	chave.append("ID_DESDOBRAMENTO:");
//		        	chave.append(idDoc.getString("ID_DESDOBRAMENTO"));
//		        	valor.append(" > ");
//		        	valor.append(doc.getString("DESCR_DESDOBRAMENTO"));
//	        	}
//	        	
//	        	if (idDoc.containsKey("ID_CIRCUNSTANCIA") && Verificador.isValorado(idDoc.getString("ID_CIRCUNSTANCIA")))
//	        	{
//		        	chave.append("|");
//		        	chave.append("ID_CIRCUNSTANCIA:");
//		        	chave.append(idDoc.getString("ID_CIRCUNSTANCIA"));
//		        	valor.append(" > ");
//		        	valor.append(doc.getString("DESCR_CIRCUNSTANCIA"));
//	        	}
//	        	
//	        	naturezas.put(chave.toString(), valor.toString());
//	        }
//	    } finally {
//	        cursor.close();
//	    }
//
//		
//		
//		return naturezas;
//	}

	
	private Map<String, String> ordenarPorValor(Map<String, String> mapaDesordenado)
	{
    	// Convert Map to List
    	List<Map.Entry<String, String>> list = new LinkedList<Map.Entry<String, String>>(mapaDesordenado.entrySet());

    	// Sort list with comparator, to compare the Map values
    	Collections.sort(list, new Comparator<Map.Entry<String, String>>() {
    		public int compare(Map.Entry<String, String> o1,
    								Map.Entry<String, String> o2) {
    				return (o1.getValue()).compareTo(o2.getValue());
    			}
    		});

    	// Convert sorted map back to a Map
    	Map<String, String> mapaOrdenado = new LinkedHashMap<String, String>();
    	for (Iterator<Map.Entry<String, String>> it = list.iterator(); it.hasNext();) {
    		Map.Entry<String, String> entry = it.next();
    		mapaOrdenado.put(entry.getKey(), entry.getValue());
    	}
    	return mapaOrdenado;
	}
	
	
	/**
	 * Substitui (replace) a ocorrencia pelo id
	 * @param ocorrencia
	 */
	public void substituir(Ocorrencia ocorrencia)
	{
		try
		{
			String id = ocorrencia.getId();
			String json = paraJson(ocorrencia);
			
			Client client = ClientBuilder.newClient();
			String servico = "http://" + host + ":"+ port + INDEX_TYPE + "{id}/_update";
			WebTarget webTarget = client.target(servico);
			Response response = webTarget
					.resolveTemplate("id", id)
					.request(MediaType.APPLICATION_JSON)
					.post(Entity.json(json));
			json = response.readEntity(String.class);
			logger.info("Resposta Elasticsearch: " + json);
		}
		catch (Exception e)
		{
			String msg = "Erro na alteracao. num[" + ocorrencia.getNumBo() + "] ano["
					+ ocorrencia.getAnoBo() + "] dlg[" + ocorrencia.getIdDelegacia() + "/"
					+ ocorrencia.getNomeDelegacia() + "] dtReg[" + ocorrencia.getDatahoraRegistroBo() + "] "
					+ "err[" + e.getMessage() + "].";
			throw new RuntimeException(msg);
		}
	}
	
	/**
	 * retorna uma referencia com o ID do banco
	 * @param ocorrencia
	 * @return
	 */
	public Ocorrencia adicionar(Ocorrencia ocorrencia)
	{
		try
		{
			String id = ocorrencia.getId();
			String json = paraJson(ocorrencia);
			
			Client client = ClientBuilder.newClient();
			String servico = "http://" + host + ":"+ port + INDEX_TYPE + "{id}";
			WebTarget webTarget = client.target(servico);
			Response response = webTarget
					.resolveTemplate("id", id)
					.request(MediaType.APPLICATION_JSON)
					.put(Entity.json(json));
			json = response.readEntity(String.class);
			logger.info("Resposta Elasticsearch: " + json);

		}
		catch (Exception e)
		{
			String msg = "Erro na adicao. num[" + ocorrencia.getNumBo() + "] ano["
					+ ocorrencia.getAnoBo() + "] dlg[" + ocorrencia.getIdDelegacia() + "/"
					+ ocorrencia.getNomeDelegacia() + "] dtReg[" + ocorrencia.getDatahoraRegistroBo() + "] "
					+ "err[" + e.getMessage() + "].";
			throw new RuntimeException(msg);
		}
		return ocorrencia;
	}
	
	private void atribuirPesquisaGeral(Ocorrencia ocorrencia)
	{
		String valorPesquisaTextual = ocorrencia.getAuxiliar().montarAtributoParaPesquisaGeral(ocorrencia);
		if (Verificador.isValorado(valorPesquisaTextual))
		{
			ocorrencia.getAuxiliar().setPesquisaGeralRegex(valorPesquisaTextual);
			ocorrencia.getAuxiliar().setPesquisaGeralTextual(valorPesquisaTextual);
		}
	}
	
	private void validarRelacionamento(Ocorrencia ocorrencia)
	{
		if (ocorrencia != null && ocorrencia.getAuxiliar() != null)
		{
			Auxiliar auxiliar = ocorrencia.getAuxiliar();
			if (auxiliar.getPai() != null)
			{
				Ocorrencia pai = auxiliar.getPai();
				if (pai.getId().equals(ocorrencia.getId()))
				{
					String msg = "Validacao de alteracao. num[" + ocorrencia.getNumBo() + "] ano["
							+ ocorrencia.getAnoBo() + "] dlg[" + ocorrencia.getIdDelegacia() + "/"
							+ ocorrencia.getNomeDelegacia() + "] dtReg[" + ocorrencia.getDatahoraRegistroBo() + "] "
							+ "err[ registro corrente possui PAI com o mesmo 'id' ].";
					throw new RuntimeException(msg);
				}
			}
			
			if (Verificador.isValorado(auxiliar.getFilhos()))
			{
				for (Ocorrencia filho : auxiliar.getFilhos())
				{
					if (filho.getId().equals(ocorrencia.getId()))
					{
						String msg = "Validacao de alteracao. num[" + ocorrencia.getNumBo() + "] ano["
								+ ocorrencia.getAnoBo() + "] dlg[" + ocorrencia.getIdDelegacia() + "/"
								+ ocorrencia.getNomeDelegacia() + "] dtReg[" + ocorrencia.getDatahoraRegistroBo() + "] "
								+ "err[ registro corrente possui FILHO com o mesmo 'id' ].";
						throw new RuntimeException(msg);
					}
				}
			}
		}
	}
	
	//////---- agregacoes
	@Log
	public Map<String, Integer> agregarPorFlagrante(final Map<String, String> filtros)
	{
		Map<String, Integer> resultado = new TreeMap<>();
		return resultado;
	}
	
	@Log
	public Map<String, Integer> agregarPorAno(final Map<String, String> filtros)
	{
//		POST
//		http://10.75.200.49:9200/rdo_roubo_carga_recep06/bo/_search?pretty
//		{
//			   "size": 0, 
//			   "aggregations": {
//			      "group_por_ano": {
//			         "terms": {
//			            "field": "ANO_BO",
//			            "order" : { "_term" : "desc" }
//			         }
//			      }
//			   }
//			}
//		
		Map<String, Integer> resultado = new TreeMap<>();
		return resultado;
	}
	
	@Log
	public Map<String, Integer> agregarPorComplementar(final Map<String, String> filtros)
	{
		Map<String, Integer> resultado = new TreeMap<>();
		return resultado;
	}
	
	private BasicDBObject montarPesquisa(Map<String, String> filtros)
	{
		BasicDBObject pesquisa = new BasicDBObject();
		
		if (Verificador.isValorado(filtros))
		{
			if (filtros.containsKey("dataInicial") || filtros.containsKey("dataFinal"))
			{
				BasicDBObject periodo = new BasicDBObject();
				if (filtros.containsKey("dataInicial"))
				{
					periodo.put("$gt", filtros.get("dataInicial") + "000000");
				}
				if (filtros.containsKey("dataFinal"))
				{
					periodo.put("$lt", filtros.get("dataFinal") + "235959");
				}
				pesquisa.put("DATAHORA_REGISTRO_BO", periodo);
			}
			if (filtros.containsKey("flagFlagrante"))
			{
				pesquisa.put("FLAG_FLAGRANTE", filtros.get("flagFlagrante"));
			}
			if (filtros.containsKey("complemento"))
			{
				String complemento = filtros.get("complemento");
				if (complemento.equalsIgnoreCase("A"))
				{	//-- ocorrencias que nao possuem complementares
					pesquisa.put("AUXILIAR.FLAG_COMPLEMENTAR_DE_NATUREZA_LOCALIZACAO", new BasicDBObject("$exists", false));
				}
				else if (complemento.equalsIgnoreCase("B"))
				{	//-- ocorrencias que possuem complementares
					pesquisa.put("AUXILIAR.FLAG_COMPLEMENTAR_DE_NATUREZA_LOCALIZACAO", new BasicDBObject("$exists", true));
				}
				else if (complemento.equalsIgnoreCase("C"))
				{	//-- apenas ocorrencias complementares 
					pesquisa.put("ANO_REFERENCIA_BO", new BasicDBObject("$exists", true));
					pesquisa.put("NATUREZA.ID_OCORRENCIA", "40");
					pesquisa.put("NATUREZA.ID_ESPECIE", "40");
				}
			}		
			if (filtros.containsKey("natureza"))
			{
				String natureza = filtros.get("natureza");
				if (natureza.equalsIgnoreCase("C"))
				{	
					pesquisa.put("NATUREZA.ID_NATUREZA", 
						new BasicDBObject("$nin", 
							Arrays.asList("180A", "180B", "180C") ));
				}
				else if (natureza.equalsIgnoreCase("R"))
				{	
					pesquisa.put("NATUREZA.ID_NATUREZA", 
						new BasicDBObject("$in", 
								Arrays.asList("180A", "180B", "180C") ));
				}
			}
			if (filtros.containsKey("latitude") && filtros.containsKey("longitude") && filtros.containsKey("raioEmMetros"))
			{
				double latitude = Double.parseDouble(filtros.get("latitude"));
				double longitude = Double.parseDouble(filtros.get("longitude"));
				int raioEmMetros = Integer.parseInt(filtros.get("raioEmMetros"));
				double[] longitudeLatitude = {longitude, latitude};
				
				BasicDBObject geometry = new BasicDBObject();
				geometry.put("type", "Point");
				geometry.put("coordinates", longitudeLatitude);
				
				BasicDBObject near = new BasicDBObject();
				near.put("$geometry", geometry);
				near.put("$maxDistance", raioEmMetros);
				
				BasicDBObject geo = new BasicDBObject();
				geo.put("$near", near);
				
				pesquisa.put("AUXILIAR.geometry", geo);
				
				//{"AUXILIAR.geometry": {$near: {$geometry: {"type":"Point", "coordinates":[-47.0621223449707, -23.44363021850586]}, $maxDistance: 13}}}
				
			}
			if (filtros.containsKey("numBo"))
			{
				pesquisa.append("NUM_BO", filtros.get("numBo"));
			}
			if (filtros.containsKey("anoBo"))
			{
				pesquisa.append("ANO_BO", filtros.get("anoBo"));
			}
			if (filtros.containsKey("idDelegacia"))
			{
				pesquisa.append("ID_DELEGACIA", filtros.get("idDelegacia"));
			}
			if (filtros.containsKey("idTipoObjeto") && filtros.containsKey("idSubtipoObjeto"))
			{
				BasicDBObject elementoComOsCampos = new BasicDBObject();
				elementoComOsCampos.append("ID_TIPO_OBJETO", filtros.get("idTipoObjeto"));
				elementoComOsCampos.append("ID_SUBTIPO_OBJETO", filtros.get("idSubtipoObjeto"));
				
				BasicDBObject elemMatch = new BasicDBObject("$elemMatch", elementoComOsCampos);

				pesquisa.append("OBJETO", elemMatch);
			}

		}
			
		return pesquisa;
	}
	
	
	private JsonObject criarMatch(String atributo, String valor)
	{
		JsonObject atrib = new JsonObject();
		atrib.addProperty(atributo, valor);
		
		JsonObject match = new JsonObject();
		match.add("match", atrib);
		
		return match;
	}
	
	private JsonObject montarPesquisa(List<FiltroCondicao> filtroCondicoes)
	{
		JsonArray must = new JsonArray();
		
		
	    //BasicDBObject dbFiltros = new BasicDBObject();
	    if (Verificador.isValorado(filtroCondicoes))
	    {
		    for (FiltroCondicao filtroCondicao : filtroCondicoes)
		    {
		    	if (filtroCondicao.getTipo().equals(TipoFiltro.TEXTO))
		    	{
//		    		if (filtroCondicao.getValorTextoTipo() != null && filtroCondicao.getValorTextoTipo().equals(TipoPesquisaTexto.REGEX))
//		    		{
//						String convertido = AcentuacaoParaRegex.converter(filtroCondicao.getValorTexto());
//						//dbFiltros.append(filtroCondicao.getAtributoDePesquisa(), new BasicDBObject("$regex", convertido).append("$options", "i"));
//						//Pattern.compile(".*myValue.*" , Pattern.CASE_INSENSITIVE)
//						dbFiltros.append(filtroCondicao.getAtributoDePesquisa(), Pattern.compile(convertido, Pattern.CASE_INSENSITIVE));
//		    		}
//		    		else
//		    		{	//-- EXATO
//		    			dbFiltros.append(filtroCondicao.getAtributoDePesquisa(), filtroCondicao.getValorTexto());
		    			JsonObject match = criarMatch(filtroCondicao.getAtributoDePesquisa(), filtroCondicao.getValorTexto());
		    			must.add(match);
//		    		}
		    	}
/*		    	
		    	if (filtroCondicao.getTipo().equals(TipoFiltro.INTERVALO_DATA))
		    	{
		    		BasicDBObject periodo = new BasicDBObject();
					if (Verificador.isValorado(filtroCondicao.getValorIntervaloDataInicial()))
					{
						periodo.put("$gt", filtroCondicao.getValorIntervaloDataInicial() + "000000");
					}
					if (Verificador.isValorado(filtroCondicao.getValorIntervaloDataFinal()))
					{
						periodo.put("$lt", filtroCondicao.getValorIntervaloDataFinal() + "235959");
					}
					dbFiltros.put(filtroCondicao.getAtributoDePesquisa(), periodo);		
		    	}
		    	if (filtroCondicao.getTipo().equals(TipoFiltro.LISTA_SELECAO_UNICA))
		    	{
		    		if (filtroCondicao.getAtributoDePesquisa().contains("COMPLEMENTAR"))
		    		{
		    			String opcao = filtroCondicao.getValorListaSelecaoUnicaIdSelecionado();
						if (opcao.equalsIgnoreCase("A"))
						{	//-- ocorrencias que nao possuem complementares
							dbFiltros.put("AUXILIAR.FLAG_COMPLEMENTAR_DE_NATUREZA_LOCALIZACAO", new BasicDBObject("$exists", false));
						}
						else if (opcao.equalsIgnoreCase("B"))
						{	//-- ocorrencias que possuem complementares
							dbFiltros.put("AUXILIAR.FLAG_COMPLEMENTAR_DE_NATUREZA_LOCALIZACAO", new BasicDBObject("$exists", true));
						}
						else if (opcao.equalsIgnoreCase("C"))
						{	//-- apenas ocorrencias complementares de recuperacao e localizacao
							dbFiltros.put("ANO_REFERENCIA_BO", new BasicDBObject("$exists", true));
							dbFiltros.put("NATUREZA.ID_OCORRENCIA", "40");
							dbFiltros.put("NATUREZA.ID_ESPECIE", "40");
						}
						else if (opcao.equalsIgnoreCase("D"))
						{	//-- apenas nao complementar
							dbFiltros.put("ANO_REFERENCIA_BO", new BasicDBObject("$exists", false));
						}
						else
						{	//-- apenas complementar
							dbFiltros.put("ANO_REFERENCIA_BO", new BasicDBObject("$exists", true));
						}
		    		}
		    		else if (filtroCondicao.getAtributoDePesquisa().contains("NATUREZA"))
		    		{
						String natureza = filtroCondicao.getValorListaSelecaoUnicaIdSelecionado();
						if (natureza.equalsIgnoreCase("CARGA"))
						{	
							dbFiltros.put("NATUREZA.ID_NATUREZA", 
								new BasicDBObject("$nin", 
									Arrays.asList("180A", "180B", "180C") ));
						}
						else if (natureza.equalsIgnoreCase("RECEPTACAO"))
						{	
							dbFiltros.put("NATUREZA.ID_NATUREZA", 
								new BasicDBObject("$in", 
										Arrays.asList("180A", "180B", "180C") ));
						}
		    		}
		    		else
		    		{
		    			dbFiltros.append(filtroCondicao.getAtributoDePesquisa(), filtroCondicao.getValorListaSelecaoUnicaIdSelecionado());
		    		}
		    	}
		    	if (filtroCondicao.getTipo().equals(TipoFiltro.ARVORE))
		    	{
		    		if (filtroCondicao.getAtributoDePesquisa().contains("OBJETO."))	
		    		{	//-- tratamento especifico para a hierarquia de objetos   
			    		//-- hierarquia com dois niveis   

		    			String campos[] = filtroCondicao.getAtributoDePesquisa().split("\\|");
		    			String campo = campos[0].substring(campos[0].lastIndexOf(".") + 1);
		    			String subCampo = campos[1].substring(campos[1].lastIndexOf(".") + 1);

		    			String chave = "";
		    			String subChave = "";
		    			ArvoreSimples arvore = filtroCondicao.getArvoreSelecao();

		    			List<BasicDBObject> listaDeCondicao = new ArrayList<>();
		    			for (ArvoreSimples nivel1 : arvore.getFilhos())
		    			{
		    				for (ArvoreSimples nivel2 : nivel1.getFilhos())
		    				{
		    					BasicDBObject elementoComOsCampos = new BasicDBObject();
		    					elementoComOsCampos.append(campo, nivel1.getChave());
		    					elementoComOsCampos.append(subCampo, nivel2.getChave());
		    					
		    					BasicDBObject condicao = new BasicDBObject("OBJETO", new BasicDBObject("$elemMatch", elementoComOsCampos));
		    					
		    					listaDeCondicao.add(condicao);
		    				}
		    			}
			    		
		    			dbFiltros.append("$or", listaDeCondicao);
		    		}
		    		
		    		if (filtroCondicao.getAtributoDePesquisa().contains("NATUREZA"))	
		    		{	//-- tratamento especifico para a hierarquia de NATUREZAS   
		    			ArvoreSimples arvore = filtroCondicao.getArvoreSelecao();
		    			List<String> chaves = new ArrayList<>();
		    			recuperarChaves(arvore, chaves);

		    			List<BasicDBObject> listaDeCondicao = new ArrayList<>();
		    			for (String chaveComposta : chaves)
		    			{
		    				BasicDBObject elemento = new BasicDBObject();
		    				String chaveArr[] = chaveComposta.split("\\|");
		    				for (String atributo : chaveArr)
		    				{
		    					String chaveValor[] = atributo.split(":");
		    					String chave = chaveValor[0];
		    					String valor = chaveValor[1];
		    					elemento.append(chave, valor);
		    				}
		    				BasicDBObject condicao = new BasicDBObject("NATUREZA", new BasicDBObject("$elemMatch", elemento));
		    				listaDeCondicao.add(condicao);
		    			}
		    			dbFiltros.append("$or", listaDeCondicao);
		    		}
		    	}
		    	
		    	if (filtroCondicao.getTipo().equals(TipoFiltro.GEO_RAIO))
		    	{
					double latitude = Double.parseDouble(filtroCondicao.getValorGeoRaioLatitude());
					double longitude = Double.parseDouble(filtroCondicao.getValorGeoRaioLongitude());
					int raioEmMetros = Integer.parseInt(filtroCondicao.getValorGeoRaioEmMetros());
					double[] longitudeLatitude = {longitude, latitude};
					
					BasicDBObject geometry = new BasicDBObject();
					geometry.put("type", "Point");
					geometry.put("coordinates", longitudeLatitude);
					
					BasicDBObject near = new BasicDBObject();
					near.put("$geometry", geometry);
					near.put("$maxDistance", raioEmMetros);
					
					BasicDBObject geo = new BasicDBObject();
					geo.put("$near", near);
					
					dbFiltros.put("AUXILIAR.geometry", geo);
		    	}
*/		    	
		    }
	    }
	    
	    JsonObject mustAtrib = new JsonObject();
	    mustAtrib.add("must", must);
	    
	    JsonObject bool = new JsonObject();
	    bool.add("bool", mustAtrib);
	    
	    return bool;
	}
	
	
	private void recuperarChaves(ArvoreSimples arvore, List<String> chaves)
	{
		if (arvore.getFilhos().isEmpty())
		{
			chaves.add(arvore.getChave());
		}
		else
		{
			for (ArvoreSimples filho : arvore.getFilhos())
			{
				recuperarChaves(filho, chaves);
			}
		}
	}

	
	
	////////-------
	
	
	public static JsonElement fromString(String json, String path)
	        throws JsonSyntaxException {
	    JsonObject obj = new GsonBuilder().create().fromJson(json, JsonObject.class);
	    String[] seg = path.split("\\.");
	    for (String element : seg) {
	        if (obj != null) {
	            JsonElement ele = obj.get(element);
	            if (!ele.isJsonObject()) 
	                return ele;
	            else
	                obj = ele.getAsJsonObject();
	        } else {
	            return null;
	        }
	    }
	    return obj;
	}
	
	
	/////---- converterssss
	
	public Ocorrencia paraObjeto(String json)
	{
		json = json.replaceFirst("novoId", "_id");
		Ocorrencia ocorrencia = converter.paraObjetoTratantoId(json, Ocorrencia.class);
		return ocorrencia;
	}
	
	public String paraJson(Ocorrencia ocorrencia)
	{
		String json = converter.paraJsonTratandoId(ocorrencia);
		json = json.replaceFirst("_id", "novoId");
		return json;
	}
	
	
}
