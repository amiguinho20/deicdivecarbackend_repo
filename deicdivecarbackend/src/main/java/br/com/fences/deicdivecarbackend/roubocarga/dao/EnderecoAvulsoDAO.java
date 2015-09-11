package br.com.fences.deicdivecarbackend.roubocarga.dao;

import static com.mongodb.client.model.Filters.eq;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.bson.Document;
import org.bson.types.ObjectId;

import br.com.fences.deicdivecarentidade.enderecoavulso.EnderecoAvulso;
import br.com.fences.fencesutils.conversor.AcentuacaoParaRegex;
import br.com.fences.fencesutils.conversor.converter.Converter;
import br.com.fences.fencesutils.formatar.FormatarData;
import br.com.fences.fencesutils.verificador.Verificador;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;

@Named 
@ApplicationScoped
public class EnderecoAvulsoDAO {     

	
	@Inject
	private Converter<EnderecoAvulso> converter;
	
	@Inject @ColecaoEnderecoAvulso
	private MongoCollection<Document> colecao;
	
	
	/**
	 * Consulta pelo id (identificador unico), o "_id"
	 * @param id
	 */
	public EnderecoAvulso consultar(final String id)
	{
	    Document documento = colecao.find(eq("_id", new ObjectId(id))).first();
	    EnderecoAvulso enderecoAvulso = converter.paraObjeto(documento, EnderecoAvulso.class);
	    return enderecoAvulso;
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
	public List<EnderecoAvulso> pesquisarLazy(final  Map<String, String> filtros, final int primeiroRegistro, final int registrosPorPagina)
	{
		List<EnderecoAvulso> enderecosAvulsos = new ArrayList<>();
		
		BasicDBObject dbFiltros = montarPesquisa(filtros);
	    MongoCursor<Document> cursor = colecao.find(dbFiltros).skip(primeiroRegistro).limit(registrosPorPagina).iterator();

		
	    try {
	        while (cursor.hasNext()) {
	        	Document documento = cursor.next();
	        	EnderecoAvulso enderecoAvulso = converter.paraObjeto(documento, EnderecoAvulso.class);
	        	enderecosAvulsos.add(enderecoAvulso);
	        }
	    } finally {
	        cursor.close();
	    }
	    
	    return enderecosAvulsos;
	}
	
	public List<EnderecoAvulso> pesquisarAtivoPorTipo(List<String> tipos)
	{
		List<EnderecoAvulso> enderecosAvulsos = new ArrayList<>(); 
		BasicDBObject pesquisa = new BasicDBObject();
		
		if (tipos != null && !tipos.isEmpty())
		{
			pesquisa.put("tipo", 
					new BasicDBObject("$in", tipos ));
			pesquisa.put("indicadorAtivo", "Sim");

			MongoCursor<Document> cursor = colecao.find(pesquisa).iterator();
			
		    try {
		        while (cursor.hasNext()) {
		        	Document documento = cursor.next();
		        	EnderecoAvulso enderecoAvulso = converter.paraObjeto(documento, EnderecoAvulso.class);
		        	enderecosAvulsos.add(enderecoAvulso);
		        }
		    } finally {
		        cursor.close();
		    }
		}
		
		return enderecosAvulsos;
	}
	
	/**
	 * Substitui (replace) o enderecoAvulso pelo id
	 * @param ocorrencia
	 */
	public void substituir(EnderecoAvulso enderecoAvulso)
	{
		try
		{
			enderecoAvulso.setUltimaAtualizacao(dataHoraCorrente());
			Document documento = converter.paraDocumento(enderecoAvulso);
			colecao.replaceOne(eq("_id", documento.get("_id")), documento);
		}
		catch (Exception e)
		{
			String msg = "Erro na alteracao. log[" + enderecoAvulso.getLogradouro() + "].";
			System.err.println(msg);
			e.printStackTrace();
			throw new RuntimeException(msg);
		}
	} 
	
	public void adicionar(EnderecoAvulso enderecoAvulso)
	{
		try
		{
			enderecoAvulso.setUltimaAtualizacao(dataHoraCorrente());
			Document documento = converter.paraDocumento(enderecoAvulso);
			colecao.insertOne(documento);
		}
		catch (Exception e)
		{
			String msg = "Erro na inclusao unica. log[" + enderecoAvulso.getLogradouro() + "].";
			System.err.println(msg);
			e.printStackTrace();
			throw new RuntimeException(msg);
		}
	}
	
	public void adicionar(List<EnderecoAvulso> enderecosAvulsos)
	{
		try
		{
			List<Document> documentos = new ArrayList<>();
			for (EnderecoAvulso enderecoAvulso : enderecosAvulsos)
			{
				enderecoAvulso.setUltimaAtualizacao(dataHoraCorrente());
				Document documento = converter.paraDocumento(enderecoAvulso);
				documentos.add(documento);
			}
			colecao.insertMany(documentos);
		}
		catch (Exception e)
		{
			String msg = "Erro na inclusão em lote. log[" + e.getMessage() + "].";
			System.err.println(msg);
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
	
	public void remover(EnderecoAvulso enderecoAvulso)
	{
		String id = enderecoAvulso.getId();
		remover(id);
	}
	
	public void remover(String id)
	{
		try
		{
			colecao.deleteOne(eq("_id", new ObjectId(id)));
		}
		catch (Exception e)
		{
			String msg = "Erro no remover. log[" + e.getMessage() + "].";
			throw new RuntimeException(msg, e);
		}
	}
	
	private String dataHoraCorrente()
	{
		String ultimaAtualizacao = FormatarData.getAnoMesDiaHoraMinutoSegundoConcatenados().format(new Date());
		return ultimaAtualizacao; 
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
					String valor = filtro.getValue().toString().trim();
					if (Verificador.isValorado(valor))
					{
						String convertido = AcentuacaoParaRegex.converter(valor);
						pesquisa.append(filtro.getKey(), new BasicDBObject("$regex", convertido).append("$options", "i"));
					}
				}
			}
		}
		return pesquisa;
	}
	
}
