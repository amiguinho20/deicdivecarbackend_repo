package br.com.fences.deicdivecarbackend.indiciado.dao;

import static com.mongodb.client.model.Filters.eq;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.log4j.Logger;
import org.bson.Document;
import org.bson.types.ObjectId;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;

import br.com.fences.deicdivecarbackend.roubocarga.dao.ColecaoIndiciado;
import br.com.fences.deicdivecarentidade.indiciado.Indiciado;
import br.com.fences.fencesutils.conversor.AcentuacaoParaRegex;
import br.com.fences.fencesutils.conversor.converter.Converter;
import br.com.fences.fencesutils.formatar.FormatarData;
import br.com.fences.fencesutils.verificador.Verificador;

@Named 
@ApplicationScoped
public class IndiciadoDAO {     

	@Inject
	private transient Logger logger;
	
	@Inject
	private Converter<Indiciado> converter;
	
	@Inject @ColecaoIndiciado
	private MongoCollection<Document> colecao;
	
	
	/**
	 * Consulta pelo id (identificador unico), o "_id"
	 * @param id
	 */
	public Indiciado consultar(final String id)
	{
	    Document documento = colecao.find(eq("_id", new ObjectId(id))).first();
	    Indiciado indiciado = converter.paraObjeto(documento, Indiciado.class);
	    return indiciado;
	}
	
	
	public Indiciado consultar(final Indiciado indiciado)
	{
		BasicDBObject pesquisa = new BasicDBObject();
		pesquisa.append("nome", indiciado.getNome());
		pesquisa.append("nomeDaMae", indiciado.getNomeDaMae());
		pesquisa.append("rg", indiciado.getRg());
		
		Document documento = colecao.find(pesquisa).first();
		Indiciado indiciadoRetorno = null;
		if (documento != null)
		{
			indiciadoRetorno = converter.paraObjeto(documento, Indiciado.class);
		}
	    return indiciadoRetorno;
	}
	
	
	/**
	 * Contagem de pesquisa 
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
	 * @return List<EnderecoAvulso> paginado
	 */
	public Set<Indiciado> pesquisarLazy(final  Map<String, String> filtros, final int primeiroRegistro, final int registrosPorPagina)
	{
		Set<Indiciado> indiciados = new LinkedHashSet<>();
		
		BasicDBObject dbFiltros = montarPesquisa(filtros);
	    MongoCursor<Document> cursor = colecao.find(dbFiltros).skip(primeiroRegistro).limit(registrosPorPagina).iterator();

		
	    try {
	        while (cursor.hasNext()) {
	        	Document documento = cursor.next();
	        	Indiciado indiciado = converter.paraObjeto(documento, Indiciado.class);
	        	indiciados.add(indiciado);
	        }
	    } finally {
	        cursor.close();
	    }
	    
	    return indiciados;
	}
	
	
	/**
	 * Substitui (replace) o indiciado pelo id
	 * @param ocorrencia
	 */
	public void substituir(Indiciado indiciado)
	{
		try
		{
			validar(indiciado);
			indiciado.setUltimaAtualizacao(FormatarData.dataHoraCorrente());
			Document documento = converter.paraDocumento(indiciado);
			colecao.replaceOne(eq("_id", documento.get("_id")), documento);
		}
		catch (Exception e)
		{
			String msg = "Erro na alteracao. log[" + indiciado + "] err[" + e.getMessage() + "].";
			logger.error(msg, e);
			throw new RuntimeException(msg);
		}
	} 
	
	public Indiciado adicionar(Indiciado indiciado)
	{
		try
		{
			validar(indiciado);
			indiciado.setUltimaAtualizacao(FormatarData.dataHoraCorrente());
			Document documento = converter.paraDocumento(indiciado);
			colecao.insertOne(documento);
			indiciado = converter.paraObjeto(documento, Indiciado.class);
		}
		catch (Exception e)
		{
			String msg = "Erro na adicao. log[" + indiciado + "] err[" + e.getMessage() + "].";
			logger.error(msg, e);
			throw new RuntimeException(msg);
		}
		return indiciado;
	}
	
	public void adicionar(Set<Indiciado> indiciados)
	{
		try
		{
			List<Document> documentos = new ArrayList<>();
			for (Indiciado indiciado : indiciados)
			{
				validar(indiciado);
				indiciado.setUltimaAtualizacao(FormatarData.dataHoraCorrente());
				Document documento = converter.paraDocumento(indiciado);
				documentos.add(documento);
			}
			colecao.insertMany(documentos);
		}
		catch (Exception e)
		{
			String msg = "Erro na inclusão em lote. log[" + e.getMessage() + "].";
			logger.error(msg, e);
			throw new RuntimeException(msg);
		}
	}
	
	private void validar(Indiciado indiciado){
		if (!Verificador.isValorado(indiciado.getNome()))
		{
			throw new IllegalArgumentException("O nome do indiciado esta vazio.");
		}
		if (!Verificador.isValorado(indiciado.getNomeDaMae()))
		{
			throw new IllegalArgumentException("O nome da mae do indiciado esta vazio.");
		}
		if (!Verificador.isValorado(indiciado.getRg()))
		{
			throw new IllegalArgumentException("O nome da mae do indiciado esta vazio.");
		}
		if (!Verificador.isValorado(indiciado.getOcorrencias()))
		{
			throw new IllegalArgumentException("O indiciado nao possui ocorrencias associadas.");
		}
	}
	
	
	public void remover(Indiciado indiciado)
	{
		try
		{
			Document documento = converter.paraDocumento(indiciado);
			colecao.deleteOne(eq("_id", documento.get("_id")));
		}
		catch (Exception e)
		{
			String msg = "Erro na remocao. log[" + indiciado + "] err[" + e.getMessage() + "].";
			logger.error(msg, e);
			throw new RuntimeException(msg);
		}
	}

	
	private BasicDBObject montarPesquisa(Map<String, String> filtros)
	{
		BasicDBObject pesquisa = new BasicDBObject();
		if (Verificador.isValorado(filtros))
		{
			for (Map.Entry<String, String> filtro : filtros.entrySet())
			{
				if (filtro.getValue() != null)
				{
					if (filtro.getKey().equals("ocorrencias.id"))
					{
						pesquisa.append(filtro.getKey(), new BasicDBObject("$in", filtro.getValue().split(";")));
					}
					else
					{
						String valor = filtro.getValue().toString().trim();
						if (Verificador.isValorado(valor))
						{
							String convertido = AcentuacaoParaRegex.converter(valor);
							pesquisa.append(filtro.getKey(), new BasicDBObject("$regex", convertido).append("$options", "i"));
						}
					}
				}
			}
		}
		return pesquisa;
	}
	
}
