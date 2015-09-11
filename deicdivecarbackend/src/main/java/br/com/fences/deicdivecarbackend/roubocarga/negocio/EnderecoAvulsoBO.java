package br.com.fences.deicdivecarbackend.roubocarga.negocio;

import java.util.List;
import java.util.Map;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import br.com.fences.deicdivecarbackend.roubocarga.dao.EnderecoAvulsoDAO;
import br.com.fences.deicdivecarentidade.enderecoavulso.EnderecoAvulso;

/**
 * Business Object
 *
 */
@RequestScoped
public class EnderecoAvulsoBO {
	
	@Inject
	private EnderecoAvulsoDAO enderecoAvulsoDAO;
	
	public EnderecoAvulso consultar(String id)
	{
		return enderecoAvulsoDAO.consultar(id);
	}
	
	public int contar(Map<String, String> filtros)
	{
		return enderecoAvulsoDAO.contar(filtros);
	}
	
	public List<EnderecoAvulso> pesquisarLazy(Map<String, String> filtros, final int primeiroRegistro, final int registrosPorPagina)
	{
		return enderecoAvulsoDAO.pesquisarLazy(filtros, primeiroRegistro, registrosPorPagina);
	}
	
	public List<EnderecoAvulso> pesquisarAtivoPorTipo(List<String> tipos)
	{
		return enderecoAvulsoDAO.pesquisarAtivoPorTipo(tipos);
	}
	
	public void substituir(EnderecoAvulso enderecoAvulso)
	{
		enderecoAvulsoDAO.substituir(enderecoAvulso);
	}
	
	public void adicionar(EnderecoAvulso enderecoAvulso)
	{
		enderecoAvulsoDAO.adicionar(enderecoAvulso);
	}

	public void adicionar(List<EnderecoAvulso> enderecosAvulsos)
	{
		enderecoAvulsoDAO.adicionar(enderecosAvulsos);
	}

	public void remover(String id)
	{
		enderecoAvulsoDAO.remover(id);
	}
	
	public void remover(EnderecoAvulso enderecoAvulso)
	{
		enderecoAvulsoDAO.remover(enderecoAvulso);
	}

}
