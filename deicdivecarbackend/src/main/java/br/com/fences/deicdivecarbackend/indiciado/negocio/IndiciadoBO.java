package br.com.fences.deicdivecarbackend.indiciado.negocio;

import java.util.Map;
import java.util.Set;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import br.com.fences.deicdivecarbackend.indiciado.dao.IndiciadoDAO;
import br.com.fences.deicdivecarentidade.indiciado.Indiciado;

/**
 * Business Object
 *
 */
@RequestScoped
public class IndiciadoBO {
	
	@Inject
	private IndiciadoDAO indiciadoDAO;
	
	public Indiciado consultar(String id)
	{
		return indiciadoDAO.consultar(id);
	}

	public Indiciado consultar(Indiciado indiciado)
	{
		return indiciadoDAO.consultar(indiciado);
	}

	
	public int contar(Map<String, String> filtros)
	{
		return indiciadoDAO.contar(filtros);
	}
	
	public Set<Indiciado> pesquisarLazy(Map<String, String> filtros, final int primeiroRegistro, final int registrosPorPagina)
	{
		return indiciadoDAO.pesquisarLazy(filtros, primeiroRegistro, registrosPorPagina, null, 0);
	}

	public Set<Indiciado> pesquisarLazy(Map<String, String> filtros, final int primeiroRegistro, final int registrosPorPagina,
			final String campoOrdenacao, final int ordem)
	{
		return indiciadoDAO.pesquisarLazy(filtros, primeiroRegistro, registrosPorPagina, campoOrdenacao, ordem);
	}

	
	public void substituir(Indiciado indiciado)
	{
		indiciadoDAO.substituir(indiciado);
	}
	
	public Indiciado adicionar(Indiciado indiciado)
	{
		indiciado = indiciadoDAO.adicionar(indiciado);
		return indiciado;
	}

	public void adicionar(Set<Indiciado> indiciados)
	{
		indiciadoDAO.adicionar(indiciados);
	}

	
	public void remover(Indiciado indiciado)
	{
		indiciadoDAO.remover(indiciado);
	}

}
