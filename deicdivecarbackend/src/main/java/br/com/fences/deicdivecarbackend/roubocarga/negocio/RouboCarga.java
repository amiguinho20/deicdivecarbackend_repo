package br.com.fences.deicdivecarbackend.roubocarga.negocio;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import br.com.fences.deicdivecarbackend.roubocarga.dao.RdoRouboCargaReceptacaoDAO;
import br.com.fences.fencesutils.verificador.Verificador;
import br.com.fences.ocorrenciaentidade.ocorrencia.Ocorrencia;
import br.com.fences.ocorrenciaentidade.ocorrencia.natureza.Natureza;

/**
 * Business Object
 *
 */
@RequestScoped
public class RouboCarga {
	
	@Inject
	private RdoRouboCargaReceptacaoDAO rouboCargaDAO;
	
	public Ocorrencia adicionar(Ocorrencia ocorrencia)
	{
		boolean existe = rouboCargaDAO.isExisteNoBanco(ocorrencia);
		if (existe)
		{
			//-- recupera o ID
			Ocorrencia ocorrenciaConsultada = rouboCargaDAO.consultar(ocorrencia);
			ocorrencia.setId(ocorrenciaConsultada.getId());
		}

		try
		{
			if (existe)
			{
				rouboCargaDAO.substituir(ocorrencia);
			}
			else
			{
				ocorrencia = rouboCargaDAO.adicionar(ocorrencia);
			}

			
			boolean atualizaRegistro = false;
			//--
			Set<Ocorrencia> filhos = new LinkedHashSet<>();
			filhos = rouboCargaDAO.consultarFilhos(ocorrencia);
			
			for (Ocorrencia filho : filhos)
			{
				if (filho.getAuxiliar().getPai() == null)
				{
					filho.getAuxiliar().setPai(ocorrencia);
					rouboCargaDAO.substituir(filho);
					ocorrencia.getAuxiliar().getFilhos().add(filho);
					atualizaRegistro = true;
				}
				else
				{
					Ocorrencia pai = filho.getAuxiliar().getPai();
					if (ocorrencia.getId().equals(pai.getId()))
					{
						//-- o id do pai no banco condiz com o id do registro atual... 
						//-- mantem a referencia
						ocorrencia.getAuxiliar().getFilhos().add(filho);
						atualizaRegistro = true;
					}
					else
					{
						String msg = "A ocorrencia[" + ocorrencia.getId() + "] possui um filho com um pai[" + pai.getId() + "] de ID diferente.";
						throw new RuntimeException(msg);
					}
				}
			}
			
			//--
			Ocorrencia pai = null;
			if (Verificador.isValorado(ocorrencia.getAnoReferenciaBo()))
			{
				//-- referencia cruzada
				pai = rouboCargaDAO.consultarPai(ocorrencia);
				if (pai != null)
				{
					//-- adiciona o PAI no FILHO
					ocorrencia.getAuxiliar().setPai(pai);

					//-- adiciona o FILHO no PAI
					pai.getAuxiliar().getFilhos().add(ocorrencia);

					//-- verifica se registro inserido(filho) possui natureza de localizacao, caso sim, sinaliza o pai
					for (Natureza natureza : ocorrencia.getNaturezas())
					{
						String idOcorrencia = natureza.getIdOcorrencia();
						String idEspecie = natureza.getIdEspecie();
						if (Verificador.isValorado(idOcorrencia, idEspecie))
						{
							if (idOcorrencia.equals("40") && idEspecie.equals("40"))
							{
								pai.getAuxiliar().setFlagComplementarDeNaturezaLocalizacao("S");
							}
						}
					}
					rouboCargaDAO.substituir(pai);
					atualizaRegistro = true;
				}
			}
			
			if (atualizaRegistro)
			{
				rouboCargaDAO.substituir(ocorrencia);
			}
			
		}
		catch (Exception e)
		{
			String msg = "Erro na regra de adicao e associacoes. num[" + ocorrencia.getNumBo() + "] ano["
					+ ocorrencia.getAnoBo() + "] dlg[" + ocorrencia.getIdDelegacia() + "/"
					+ ocorrencia.getNomeDelegacia() + "] dtReg[" + ocorrencia.getDatahoraRegistroBo() + "] "
					+ "err[" + e.getMessage() + "].";
			throw new RuntimeException(msg);
		}	
		
		return ocorrencia;
		
		
		
		
		
		
		
		
		
		
		
/*		
		try
		{
			Ocorrencia pai = null;
			if (Verificador.isValorado(ocorrencia.getAnoReferenciaBo()))
			{
				//-- referencia cruzada 1/2, id do pai no filho(registro atual)
				pai = rouboCargaDAO.consultarPai(ocorrencia);
				if (pai != null)
				{
					ocorrencia.getAuxiliar().setPai(pai);
				}
			}
			
			if (existe)
			{
				rouboCargaDAO.substituir(ocorrencia);
			}
			else
			{
				rouboCargaDAO.adicionar(ocorrencia);
			}
		
			
			if (pai != null)
			{
				//-- referencia cruzada 2/2, id do filho no pai
				Ocorrencia ocorrenciaRecemAdicionada = rouboCargaDAO.consultar(ocorrencia);
				pai.getAuxiliar().getFilhos().add(ocorrenciaRecemAdicionada);
				
				//-- verifica se registro inserido(filho) possui natureza de localizacao, caso sim, sinaliza o pai
				for (Natureza natureza : ocorrenciaRecemAdicionada.getNaturezas())
				{
					String idOcorrencia = natureza.getIdOcorrencia();
					String idEspecie = natureza.getIdEspecie();
					if (Verificador.isValorado(idOcorrencia, idEspecie))
					{
						if (idOcorrencia.equals("40") && idEspecie.equals("40"))
						{
							pai.getAuxiliar().setFlagComplementarDeNaturezaLocalizacao("S");
						}
					}
				}
				rouboCargaDAO.substituir(pai);
			}
			
		}
		catch (Exception e)
		{
			String msg = "Erro na regra de adicao e associacoes. num[" + ocorrencia.getNumBo() + "] ano["
					+ ocorrencia.getAnoBo() + "] dlg[" + ocorrencia.getIdDelegacia() + "/"
					+ ocorrencia.getNomeDelegacia() + "] dtReg[" + ocorrencia.getDatahoraRegistroBo() + "] "
					+ "err[" + e.getMessage() + "].";
			throw new RuntimeException(msg);
		}
*/
		
		
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
	
	public String pesquisarPrimeiraDataRegistro()
	{
		return rouboCargaDAO.pesquisarPrimeiraDataRegistro();
	}
	
	public String pesquisarUltimaDataRegistro()
	{
		return rouboCargaDAO.pesquisarUltimaDataRegistro();
	}

	public List<String> listarAnos()
	{
		return rouboCargaDAO.listarAnos();
	}
	
	public Map<String, String> listarDelegacias()
	{
		return rouboCargaDAO.listarDelegacias();
	}
}
