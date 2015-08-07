package br.com.fences.deicdivecarbackend.roubocarga.dao;

import static com.mongodb.client.model.Filters.eq;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.bson.Document;
import org.bson.types.ObjectId;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;

import br.com.fences.deicdivecarbackend.config.Log;
import br.com.fences.fencesutils.conversor.mongodb.Converter;
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
	 * Consulta pelo id (identificador unico), o "_id"
	 * @param id
	 */
	public Ocorrencia consultar(final String id)
	{
	    Document documento = colecao.find(eq("_id", new ObjectId(id))).first();
	    Ocorrencia ocorrencia = converter.paraObjeto(documento);
	    consultarComplementares(ocorrencia);
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
        	List<Ocorrencia> filhos = new ArrayList<>();
        	for (Ocorrencia filho : auxiliar.getFilhos())
        	{
        		filho = consultar(filho.getId());
        		filhos.add(filho);
        	}
        	auxiliar.setFilhos(filhos);
    	}
	}

	public boolean isExisteNoBanco(Ocorrencia ocorrencia)
	{
		boolean existe = false;
		if (Verificador.isValorado(ocorrencia.getId()))
		{
			if (consultar(ocorrencia.getId()) != null)
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
		Document documento = colecao.find().first();
		Ocorrencia ocorrenciaConsultada = converter.paraObjeto(documento);
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
			Document documento = colecao.find().first();
			pai = converter.paraObjeto(documento);
		}
		return pai;
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
	        	Ocorrencia ocorrencia = converter.paraObjeto(documento);
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
	
	
	
	/**
	 * Substitui (replace) a ocorrencia pelo id
	 * @param ocorrencia
	 */
	public void substituir(Ocorrencia ocorrencia)
	{
		try
		{
			ocorrencia.getAuxiliar().setDataProcessamento(FormatarData.dataHoraCorrente());
			Document documento = converter.paraDocumento(ocorrencia);
			colecao.replaceOne(eq("_id", documento.get("_id")), documento);
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
	
	public void adicionar(Ocorrencia ocorrencia)
	{
		try
		{
			ocorrencia.getAuxiliar().setDataProcessamento(FormatarData.dataHoraCorrente());
			Document documento = converter.paraDocumento(ocorrencia);
			colecao.insertOne(documento);
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
	
	
	///////--- batch
	
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
		}	
		return pesquisa;
	}
	
}
