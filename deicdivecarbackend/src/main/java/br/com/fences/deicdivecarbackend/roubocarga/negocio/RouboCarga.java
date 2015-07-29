package br.com.fences.deicdivecarbackend.roubocarga.negocio;

import java.util.List;
import java.util.Map;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import br.com.fences.deicdivecarbackend.roubocarga.dao.RdoRouboCargaReceptacaoDAO;
import br.com.fences.ocorrenciaentidade.ocorrencia.Ocorrencia;

/**
 * Business Object
 *
 */
@RequestScoped
public class RouboCarga {
	
	@Inject
	private RdoRouboCargaReceptacaoDAO rouboCargaDAO;
	
	public void adicionar(Ocorrencia ocorrencia)
	{
		rouboCargaDAO.adicionar(ocorrencia);
	}
	
	public Map<String, Integer> agregarPorAno(Map<String, String> filtros)
	{
		return rouboCargaDAO.agregarPorAno(filtros);
	}
	
	public Map<String, Integer> agregarPorComplementar(Map<String, String> filtros)
	{
		return rouboCargaDAO.agregarPorComplementar(filtros);
	}
	
	public Map<String, Integer> agregarPorFlagrante(Map<String, String> filtros)
	{
		return rouboCargaDAO.agregarPorFlagrante(filtros);
	}
	
	public Ocorrencia consultar(String id)
	{
		return rouboCargaDAO.consultar(id);
	}

	public int contar(Map<String, String> filtros)
	{
		return rouboCargaDAO.contar(filtros);
	}
	
	public List<Ocorrencia> pesquisarLazy(Map<String, String> filtros, int primeiroRegistro, int registrosPorPagina)
	{
		return rouboCargaDAO.pesquisarLazy(filtros, primeiroRegistro, registrosPorPagina);
	}
	
	public String pesquisarUltimaDataRegistroNaoComplementar()
	{
		return rouboCargaDAO.pesquisarUltimaDataRegistroNaoComplementar();
	}
	
	public void substituir(Ocorrencia ocorrencia)
	{
		rouboCargaDAO.substituir(ocorrencia);
	}
}
