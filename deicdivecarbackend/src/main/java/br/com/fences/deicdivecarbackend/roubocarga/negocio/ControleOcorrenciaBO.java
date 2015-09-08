package br.com.fences.deicdivecarbackend.roubocarga.negocio;

import java.util.Set;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import br.com.fences.deicdivecarbackend.roubocarga.dao.ControleOcorrenciaDAO;
import br.com.fences.ocorrenciaentidade.controle.ControleOcorrencia;

/**
 * Business Object
 *
 */
@RequestScoped
public class ControleOcorrenciaBO {

	@Inject
	private ControleOcorrenciaDAO controleOcorrenciaDAO;

	public String pesquisarUltimaDataRegistroNaoComplementar() 
	{
		String ultimaDataRegistro = "";
		ultimaDataRegistro = controleOcorrenciaDAO.pesquisarUltimaDataRegistroNaoComplementar();
		return ultimaDataRegistro;
	}

	public void adicionar(ControleOcorrencia controleOcorrencia) {
		controleOcorrenciaDAO.adicionar(controleOcorrencia);
	}

	public Set<ControleOcorrencia> pesquisarProcessarReprocessar() {
		Set<ControleOcorrencia> controleOcorrencias = null;
		controleOcorrencias = controleOcorrenciaDAO.pesquisarProcessarReprocessar();
		return controleOcorrencias;
	}

	public Set<ControleOcorrencia> pesquisarIndiciadosProcessarReprocessar() {
		Set<ControleOcorrencia> controleOcorrencias = null;
		controleOcorrencias = controleOcorrenciaDAO.pesquisarIndiciadosProcessarReprocessar();
		return controleOcorrencias;
	}

	
	public void substituir(ControleOcorrencia controleOcorrencia) {
		controleOcorrenciaDAO.substituir(controleOcorrencia);
	}

}
