package br.com.fences.deicdivecarbackend.roubocarga.dao;

import static com.mongodb.client.model.Filters.eq;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.bson.Document;
import org.bson.types.ObjectId;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;

import br.com.fences.deicdivecarbackend.config.Log;
import br.com.fences.fencesutils.conversor.converter.Converter;
import br.com.fences.fencesutils.formatar.FormatarData;
import br.com.fences.fencesutils.verificador.Verificador;
import br.com.fences.ocorrenciaentidade.ocorrencia.Ocorrencia;
import br.com.fences.ocorrenciaentidade.ocorrencia.auxiliar.Auxiliar;

@ApplicationScoped
public class RdoRouboCargaReceptacaoDAO {     

	private Logger logger =  LogManager.getLogger(RdoRouboCargaReceptacaoDAO.class);
	
	@Inject
	private Converter<Ocorrencia> converter;
		
	@Inject @ColecaoRouboCarga
	private MongoCollection<Document> colecao;
	
	
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
	    Document documento = colecao.find(eq("_id", new ObjectId(id))).first();
	    Ocorrencia ocorrencia = null;
	    if (documento == null){
	    	logger.info("a consultaSemAssociacao para o id[" + id + "] retornou nulo.");
	    }
	    else
	    {
	    	ocorrencia = converter.paraObjeto(documento, Ocorrencia.class);
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
		if (Verificador.isValorado(ocorrencia.getId()))
		{
			if (consultarSemAssociacao(ocorrencia.getId()) != null)
			{
				existe = true;
			}
		}
		else
		{
			BasicDBObject pesquisa = new BasicDBObject(); 
			pesquisa.put("ID_DELEGACIA", ocorrencia.getIdDelegacia());
			pesquisa.put("ANO_BO", ocorrencia.getAnoBo());
			pesquisa.put("NUM_BO", ocorrencia.getNumBo());
			Document documento = colecao.find(pesquisa).first();
			if (documento != null)
			{
				existe = true;
			}
		}
		return existe;
	}
	
	public Ocorrencia consultar(Ocorrencia ocorrencia)
	{
		BasicDBObject pesquisa = new BasicDBObject();
		pesquisa.put("ID_DELEGACIA", ocorrencia.getIdDelegacia());
		pesquisa.put("ANO_BO", ocorrencia.getAnoBo());
		pesquisa.put("NUM_BO", ocorrencia.getNumBo());
		Document documento = colecao.find(pesquisa).first();
		Ocorrencia ocorrenciaConsultada = converter.paraObjeto(documento, Ocorrencia.class);
		return ocorrenciaConsultada;
	}
	
	public Ocorrencia consultarPai(Ocorrencia filho)
	{
		Ocorrencia pai = null;
		if (Verificador.isValorado(filho.getAnoReferenciaBo()))
		{
			BasicDBObject pesquisa = new BasicDBObject();
			pesquisa.put("ID_DELEGACIA", filho.getDelegReferenciaBo());
			pesquisa.put("ANO_BO", filho.getAnoReferenciaBo());
			pesquisa.put("NUM_BO", filho.getNumReferenciaBo());
			Document documento = colecao.find(pesquisa).first(); 
			if (documento != null)
			{
				pai = converter.paraObjeto(documento, Ocorrencia.class);
			}
		}
		return pai;
	}
	
	public Set<Ocorrencia> consultarFilhos(Ocorrencia pai)
	{
		Set<Ocorrencia> filhos = new LinkedHashSet<>();
		
		BasicDBObject pesquisa = new BasicDBObject();
		pesquisa.put("DELEG_REFERENCIA_BO", pai.getIdDelegacia());
		pesquisa.put("ANO_REFERENCIA_BO", pai.getAnoBo());
		pesquisa.put("NUM_REFERENCIA_BO", pai.getNumBo());
		
	    MongoCursor<Document> cursor = colecao.find(pesquisa).iterator();
	    
	    try {
	        while (cursor.hasNext()) {
	        	Document documento = cursor.next();
	        	Ocorrencia ocorrencia = converter.paraObjeto(documento, Ocorrencia.class);
	        	consultarPaiRef(ocorrencia);
	        	filhos.add(ocorrencia);
	        }
	    } finally {
	        cursor.close();
	    }
	    return filhos;
	}
	
	/**
	 * @param pesquisa
	 * @return count
	 */
	public int contar(final Map<String, String> filtros)
	{
		BasicDBObject dbFiltros = montarPesquisa(filtros);
	    long countL = colecao.count(dbFiltros);
	    int countI = (int) countL;
	    
	    return countI;
	}	
	
	/**
	 * Pesquisa com <b>PAGINACAO</b>
	 * @param pesquisa
	 * @param primeiroRegistro
	 * @param registrosPorPagina
	 * @return List<Ocorrencia> paginado
	 */
	@Log
	public List<Ocorrencia> pesquisarLazy(final Map<String, String> filtros, final int primeiroRegistro, final int registrosPorPagina)
	{
		List<Ocorrencia> ocorrencias = new ArrayList<>();
		
		//-- filtros
//		BasicDBObject search = new BasicDBObject("$search", pesquisa);
//	    BasicDBObject text = new BasicDBObject("$text", search); 
	    
	    //-- ordenacoes
	    BasicDBObject ordenacao = new BasicDBObject("DATAHORA_REGISTRO_BO", -1); 
	    
	   // db.rdo_roubo_carga_receptacao.find({"DATAHORA_REGISTRO_BO":{$gt:"20051229000000", $lt:"20051229999999"}},{"_id":0, "ANO_BO":1, "NUM_BO":1, "ID_DELEGACIA":1, "DATAHORA_REGISTRO_BO":1}).sort({"DATAHORA_REGISTRO_BO": -1}).count();
	    BasicDBObject dbFiltros = montarPesquisa(filtros);
	    MongoCursor<Document> cursor = colecao.find(dbFiltros).sort(ordenacao).skip(primeiroRegistro).limit(registrosPorPagina).iterator();
	    
	    try {
	        while (cursor.hasNext()) {
	        	Document documento = cursor.next();
	        	Ocorrencia ocorrencia = converter.paraObjeto(documento, Ocorrencia.class);
	        	consultarPaiRef(ocorrencia);
	        	consultarComplementares(ocorrencia);
	        	//pesquisarOcorrenciaComplementar(ocorrencia);
	        	ocorrencias.add(ocorrencia);
	        }
	    } finally {
	        cursor.close();
	    }
	    
	    return ocorrencias;
	}
	
	public String pesquisarUltimaDataRegistroNaoComplementar()
	{
		String datahoraRegistroBo = null;
		
		BasicDBObject pesquisa = new BasicDBObject("ANO_REFERENCIA_BO", new BasicDBObject("$exists", false));
		BasicDBObject projecao = new BasicDBObject("DATAHORA_REGISTRO_BO", 1).append("_id", 0);
		BasicDBObject ordenacao = new BasicDBObject("DATAHORA_REGISTRO_BO", -1);
		
		MongoCursor<Document> cursor = colecao.find(pesquisa).projection(projecao).sort(ordenacao).limit(1).iterator();
	
	    try {
	        if (cursor.hasNext()) {
	        	Document documento = cursor.next();
	        	datahoraRegistroBo = documento.getString("DATAHORA_REGISTRO_BO");
	        }
	    } finally {
	        cursor.close();
	    }
	    return datahoraRegistroBo;
	}
	
	public String pesquisarPrimeiraDataRegistro()
	{
		return pesquisarPrimeiraDataRegistro(true);
	}
	
	public String pesquisarUltimaDataRegistro()
	{
		return pesquisarPrimeiraDataRegistro(false);
	}
	
	private String pesquisarPrimeiraDataRegistro(boolean primeiro)
	{
		String datahoraRegistroBo = null;
		
		int ordem = -1;
		if (primeiro)
		{
			ordem = 1;
		}
		
		BasicDBObject pesquisa = new BasicDBObject();
		BasicDBObject projecao = new BasicDBObject("DATAHORA_REGISTRO_BO", 1).append("_id", 0);
		BasicDBObject ordenacao = new BasicDBObject("DATAHORA_REGISTRO_BO", ordem);
		
		Document documento = colecao.find(pesquisa).projection(projecao).sort(ordenacao).first();
	
        if (documento != null) {
        	datahoraRegistroBo = documento.getString("DATAHORA_REGISTRO_BO");
        }

	    return datahoraRegistroBo;
	}
	
	public List<String> listarAnos()
	{
		List<String> anos = new ArrayList<>();
		
		MongoCursor<String> cursor = colecao.distinct("ANO_BO", String.class).iterator();
		
	    try {
	        while (cursor.hasNext()) {
	        	String valor = cursor.next();
	        	anos.add(valor);
	        }
	    } finally {
	        cursor.close();
	    }
	    Collections.sort(anos);
	    Collections.reverse(anos);
	    
		return anos;
	}
	
	public Map<String, String> listarDelegacias()
	{
		Map<String, String> delegacias = new LinkedHashMap<>();
		
		//MongoCursor<String> cursor = colecao.distinct("ANO_BO", String.class).iterator();
		
		Map<String, Object> dbObjIdMap = new HashMap<String, Object>();
		dbObjIdMap.put("ID_DELEGACIA", "$ID_DELEGACIA");
		dbObjIdMap.put("NOME_DELEGACIA", "$NOME_DELEGACIA");
		DBObject groupFields = new BasicDBObject( "_id", new BasicDBObject(dbObjIdMap));
		
		MongoCursor<Document> cursor = colecao.aggregate(Arrays.asList(new Document("$group", groupFields))).iterator();
		
		
	    try {
	        while (cursor.hasNext()) {
	        	Document documento = cursor.next();
	        	Document idDoc = (Document) documento.get("_id");
	        	String chave = idDoc.getString("ID_DELEGACIA");
	        	String valor = idDoc.getString("NOME_DELEGACIA");
	        	delegacias.put(chave, valor);
	        }
	    } finally {
	        cursor.close();
	    }
	    
	    //-- ordernar o map por valor
	    {
	    	Map<String, String> mapaDesordenado = delegacias;
	    		
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
	    	delegacias = mapaOrdenado;
	    }	    
		return delegacias;
	}
	
	
	/**
	 * Substitui (replace) a ocorrencia pelo id
	 * @param ocorrencia
	 */
	public void substituir(Ocorrencia ocorrencia)
	{
		validarRelacionamento(ocorrencia);
		try
		{
			ocorrencia.getAuxiliar().setDataProcessamento(FormatarData.dataHoraCorrente());
			Document documento = converter.paraDocumento(ocorrencia);
			colecao.replaceOne(Filters.eq("_id", documento.get("_id")), documento);
//			Bson filtros = Filters.and(
//					Filters.eq("_id", documento.get("_id")), 
//					Filters.eq("ANO_BO", documento.get("ANO_BO")),
//					Filters.eq("NUM_BO", documento.get("NUM_BO")),
//					Filters.eq("ID_DELEGACIA", documento.get("ID_DELEGACIA")),
//					Filters.eq("DATAHORA_REGISTRO_BO", documento.get("DATAHORA_REGISTRO_BO"))
//			);
//			colecao.replaceOne(filtros, documento);
			//colecao.updateOne(eq("_id", documento.get("_id")), documento);
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
		validarRelacionamento(ocorrencia);
		try
		{
			ocorrencia.getAuxiliar().setDataProcessamento(FormatarData.dataHoraCorrente());
			Document documento = converter.paraDocumento(ocorrencia);
			colecao.insertOne(documento);
			ocorrencia = converter.paraObjeto(documento, Ocorrencia.class);
			return ocorrencia;
		}
		catch (Exception e)
		{
			String msg = "Erro na adicao. num[" + ocorrencia.getNumBo() + "] ano["
					+ ocorrencia.getAnoBo() + "] dlg[" + ocorrencia.getIdDelegacia() + "/"
					+ ocorrencia.getNomeDelegacia() + "] dtReg[" + ocorrencia.getDatahoraRegistroBo() + "] "
					+ "err[" + e.getMessage() + "].";
			throw new RuntimeException(msg);
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
		//aggregate(Arrays.asList(
		//        new Document("$match", new Document("borough", "Queens").append("cuisine", "Brazilian")),
		//        new Document("$group", new Document("_id", "$address.zipcode").append("count", new Document("$sum", 1)))));
		
		BasicDBObject dbFiltros = montarPesquisa(filtros);
		
		Map<String, Integer> resultado = new TreeMap<>();
		BasicDBObject match = new BasicDBObject("$match", dbFiltros);
		
		BasicDBObject agrupamento = new BasicDBObject();
		agrupamento.append("_id", "$FLAG_FLAGRANTE");
		agrupamento.append("quantidade", new BasicDBObject("$sum", 1));
		
		BasicDBObject group = new BasicDBObject("$group", agrupamento);
		
		
		MongoCursor<Document> cursor = colecao.aggregate(Arrays.asList(match, group)).iterator();
		while (cursor.hasNext()) 
		{
			Document doc = cursor.next();
			
			String chave = doc.getString("_id");
			Integer valor = doc.getInteger("quantidade", 0);
			
			resultado.put(chave, valor);
		}
		return resultado;
	}
	
	@Log
	public Map<String, Integer> agregarPorAno(final Map<String, String> filtros)
	{
		//aggregate(Arrays.asList(
		//        new Document("$match", new Document("borough", "Queens").append("cuisine", "Brazilian")),
		//        new Document("$group", new Document("_id", "$address.zipcode").append("count", new Document("$sum", 1)))));
		
		BasicDBObject dbFiltros = montarPesquisa(filtros);
		
		Map<String, Integer> resultado = new TreeMap<>();
		BasicDBObject match = new BasicDBObject("$match", dbFiltros);
		
		BasicDBObject agrupamento = new BasicDBObject();
		agrupamento.append("_id", "$ANO_BO");
		agrupamento.append("quantidade", new BasicDBObject("$sum", 1));
		
		BasicDBObject group = new BasicDBObject("$group", agrupamento);
		
		
		MongoCursor<Document> cursor = colecao.aggregate(Arrays.asList(match, group)).iterator();
		while (cursor.hasNext()) 
		{
			Document doc = cursor.next();
			
			String chave = doc.getString("_id");
			Integer valor = doc.getInteger("quantidade", 0);
			
			resultado.put(chave, valor);
		}
		return resultado;
	}
	
	@Log
	public Map<String, Integer> agregarPorComplementar(final Map<String, String> filtros)
	{
		//aggregate(Arrays.asList(
		//        new Document("$match", new Document("borough", "Queens").append("cuisine", "Brazilian")),
		//        new Document("$group", new Document("_id", "$address.zipcode").append("count", new Document("$sum", 1)))));
		
		BasicDBObject dbFiltros = montarPesquisa(filtros);
		
		Map<String, Integer> resultado = new TreeMap<>();
		BasicDBObject match = new BasicDBObject("$match", dbFiltros);
		
		BasicDBObject agrupamento = new BasicDBObject();
		agrupamento.append("_id", "$AUXILIAR.FLAG_COMPLEMENTAR_DE_NATUREZA_LOCALIZACAO");
		agrupamento.append("quantidade", new BasicDBObject("$sum", 1));
		
		BasicDBObject group = new BasicDBObject("$group", agrupamento);
		
		
		MongoCursor<Document> cursor = colecao.aggregate(Arrays.asList(match, group)).iterator();
		while (cursor.hasNext()) 
		{
			Document doc = cursor.next();
			
			String chave = doc.getString("_id");
			if (chave == null || chave.equals("null"))
			{
				chave = "Não";
			}
			if (chave.equals("S"))
			{
				chave = "Sim";
			}
			Integer valor = doc.getInteger("quantidade", 0);
			
			resultado.put(chave, valor);
		}
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
		}
			
		return pesquisa;
	}
	
}
