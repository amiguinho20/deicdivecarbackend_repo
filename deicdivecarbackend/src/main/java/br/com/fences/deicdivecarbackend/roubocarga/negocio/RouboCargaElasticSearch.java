package br.com.fences.deicdivecarbackend.roubocarga.negocio;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import br.com.fences.deicdivecarbackend.config.AppConfig;
import br.com.fences.deicdivecarbackend.roubocarga.dao.RdoRouboCargaReceptacaoDAO;
import br.com.fences.deicdivecarbackend.roubocarga.dao.RdoRouboCargaReceptacaoElasticSearchDAO;
import br.com.fences.fencesutils.filtrocustom.ArvoreSimples;
import br.com.fences.fencesutils.filtrocustom.FiltroCondicao;
import br.com.fences.fencesutils.verificador.Verificador;
import br.com.fences.ocorrenciaentidade.composto.OcorrenciaResultadoComposto;
import br.com.fences.ocorrenciaentidade.ocorrencia.Ocorrencia;
import br.com.fences.ocorrenciaentidade.ocorrencia.natureza.Natureza;

/**
 * Business Object
 *
 */
@RequestScoped
public class RouboCargaElasticSearch {
	
	@Inject
	private RdoRouboCargaReceptacaoDAO rouboCargaDAO;
	
	@Inject
	private AppConfig appConfig;
	
	@Inject
	private RdoRouboCargaReceptacaoElasticSearchDAO esDAO;
	
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
				substituir(ocorrencia);
			}
			else
			{
				ocorrencia = rouboCargaDAO.adicionar(ocorrencia);
				esAdicionar(ocorrencia);
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
					substituir(filho);
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
					substituir(pai);
					atualizaRegistro = true;
				}
			}
			
			if (atualizaRegistro)
			{
				substituir(ocorrencia);
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
		Ocorrencia ocorrencia = esConsultar(id);
		if (ocorrencia == null)
		{
			ocorrencia = rouboCargaDAO.consultar(id);
		}
		return ocorrencia;
	}

	public int contar(Map<String, String> filtros)
	{
		return rouboCargaDAO.contar(filtros);
	}
	
	public int contarDinamico(List<FiltroCondicao> filtroCondicoes)
	{
		int count = 0;
		if (Boolean.parseBoolean(appConfig.getElasticSearchSelect()))
		{
			count = esDAO.contarDinamico(filtroCondicoes);
		}
		else
		{
			count = rouboCargaDAO.contarDinamico(filtroCondicoes);
		}
		return count;
	}
	
	public List<Ocorrencia> pesquisarLazy(Map<String, String> filtros, int primeiroRegistro, int registrosPorPagina)
	{
		return rouboCargaDAO.pesquisarLazy(filtros, primeiroRegistro, registrosPorPagina);
	}
	
	public List<Ocorrencia> pesquisarDinamicoLazy(List<FiltroCondicao> filtroCondicoes, int primeiroRegistro,
			int registrosPorPagina) {
		
		List<Ocorrencia> ocorrencias = null;
		if (Boolean.parseBoolean(appConfig.getElasticSearchSelect()))
		{
			ocorrencias = esDAO.pesquisarDinamicoLazy(filtroCondicoes, primeiroRegistro, registrosPorPagina);
		}
		else
		{
			ocorrencias = rouboCargaDAO.pesquisarDinamicoLazy(filtroCondicoes, primeiroRegistro, registrosPorPagina);
		}
		return ocorrencias;
	}
	
	public OcorrenciaResultadoComposto pesquisarDinamicoLazyComposto(List<FiltroCondicao> filtroCondicoes, int primeiroRegistro,
			int registrosPorPagina) {
		
		OcorrenciaResultadoComposto orc = esDAO.pesquisarDinamicoLazyComposto(filtroCondicoes, primeiroRegistro, registrosPorPagina);
		return orc;
	}
	
	public String pesquisarUltimaDataRegistroNaoComplementar()
	{
		return rouboCargaDAO.pesquisarUltimaDataRegistroNaoComplementar();
	}
	
	public void substituir(Ocorrencia ocorrencia)
	{
		rouboCargaDAO.substituir(ocorrencia);
		esSubstituir(ocorrencia);
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
		List<String> anos = new ArrayList<>();
		anos = esListarAnos();
		if (!Verificador.isValorado(anos))
		{
			anos = rouboCargaDAO.listarAnos();
		}
		return anos;
	}
	
	public Map<String, String> listarAnosMap()
	{
		List<String> lista = listarAnos();
		Map<String, String> map = new LinkedHashMap<>();
		for (String valor : lista)
		{
			map.put(valor, valor);
		}
		return map;
	}
	
	public Map<String, String> listarDelegacias()
	{
		Map<String, String> delegacias = null;
		delegacias = esListarDelegacias();
		if (!Verificador.isValorado(delegacias))
		{
			delegacias = rouboCargaDAO.listarDelegacias();
		}
		return delegacias;
	}
	
	public Map<String, String> listarNaturezas()
	{
		return rouboCargaDAO.listarNaturezas();
	}
	
	public ArvoreSimples listarNaturezasArvore()
	{
		ArvoreSimples arvore = new ArvoreSimples();
		arvore.setChave("0");
		arvore.setValor("Raiz - naturezas");
		
		Map<String, String> mapNaturezas = rouboCargaDAO.listarNaturezas();
		
		for (Map.Entry<String, String> entry : mapNaturezas.entrySet())
		{
			String chaveComposta[] = entry.getKey().split("\\|");
			String valorComposto[] = entry.getValue().split(">");
			
			int nivel = 0;
			
			alimentarArvore(arvore, nivel, chaveComposta, valorComposto);
		}
		
		return arvore;
	}
	
	public ArvoreSimples listarNaturezasArvoreComDesdobramentoCircunstancia()
	{
		ArvoreSimples arvore = new ArvoreSimples();
		arvore.setChave("0");
		arvore.setValor("Raiz - naturezas");
		
		Map<String, String> mapNaturezas = rouboCargaDAO.listarNaturezasComDesdobramentoCircunstancia();
		
		for (Map.Entry<String, String> entry : mapNaturezas.entrySet())
		{
			String chaveComposta[] = entry.getKey().split("\\|");
			String valorComposto[] = entry.getValue().split(">");
			
			int nivel = 0;
			
			alimentarArvore(arvore, nivel, chaveComposta, valorComposto);
		}
		
		return arvore;
	}
	
	/**
	 * RECURSIVO!
	 * 
	 * Navega na arvore conforme o nivel de profundidade e insere o elemento na hierarquia e nivel adequados.
	 * @param arvore
	 * @param nivel
	 * @param chaveComposta
	 * @param valorComposto
	 */
	private void alimentarArvore(ArvoreSimples arvore, int nivel, String[] chaveComposta, String[] valorComposto)
	{
		if (nivel < chaveComposta.length)
		{
			String chave = montarChaveHierarquica(chaveComposta, nivel);
			String valor = valorComposto[nivel].trim();
			ArvoreSimples filho = recuperarElemento(arvore, chave, valor);
			if (filho == null)
			{
				filho = new ArvoreSimples(chave, valor);
				arvore.getFilhos().add(filho);
			}
			nivel++; 
			alimentarArvore(filho, nivel, chaveComposta, valorComposto);
		
		}
	}
	
	private ArvoreSimples recuperarElemento(ArvoreSimples arvore, String chave, String valor)
	{
		ArvoreSimples elementoRetorno = null;
		if (arvore.getFilhos().contains(new ArvoreSimples(chave, valor)))
		{
			for (ArvoreSimples elemento : arvore.getFilhos())
			{
				if (elemento.getChave().equals(chave))
				{
					elementoRetorno = elemento;
				}
			}
		}
		return elementoRetorno;
	}
	
	private String montarChaveHierarquica(String[] chaves, int nivel)
	{
		StringBuffer chaveHierarquica = new StringBuffer();
		
		if (chaves != null && chaves.length > 0)
		{
			for (int indice = 0; indice <= nivel; indice++)
			{
				String chave = chaves[indice];
				if (!chaveHierarquica.toString().isEmpty())
				{
					chaveHierarquica.append("|");
				}
				chaveHierarquica.append(chave);
			}
		}
		
		return chaveHierarquica.toString();
	}
	
	public Map<String, String> listarTipoPessoas()
	{
		return rouboCargaDAO.listarTipoPessoas();
	}
	
	public Map<String, String> listarTipoObjetos()
	{
		return rouboCargaDAO.listarTipoObjetos();
	}

	public ArvoreSimples listarTipoObjetosArvore()
	{
		ArvoreSimples arvore = new ArvoreSimples();
		arvore.setChave("0");
		arvore.setValor("Raiz - tipo objeto");
		Set<ArvoreSimples> nivel1 = arvore.getFilhos();
		
		//-- ordenacao por descricao
		Map<String, String> mapTipoObjetos = rouboCargaDAO.listarTipoObjetos();
		
		for (Map.Entry<String, String> entry : mapTipoObjetos.entrySet())
		{
			String chaveComposta[] = entry.getKey().split("\\|");
			String valorComposto[] = entry.getValue().split(">");
			
			String chave = chaveComposta[0];
			String subChave = chaveComposta[1];
			String valor = valorComposto[0].trim();
			String subValor = valorComposto[1].trim();
			
			
			if (nivel1.contains(new ArvoreSimples(chave, valor)))
			{
				for (ArvoreSimples elementoNivel1 : nivel1)
				{
					if (elementoNivel1.getChave().equals(chave))
					{
						ArvoreSimples elementoNivel2 = new ArvoreSimples(subChave, subValor);
						elementoNivel1.getFilhos().add(elementoNivel2);
					}
				}
			}
			else
			{
				ArvoreSimples elementoNivel1 = new ArvoreSimples(chave, valor);
				ArvoreSimples elementoNivel2 = new ArvoreSimples(subChave, subValor);
				elementoNivel1.getFilhos().add(elementoNivel2);
				nivel1.add(elementoNivel1);
			}
			
		}
		
		return arvore;
	}

	//--------- ELASTICSEARCH -----------------------------------------------------
	
	private void esSubstituir(Ocorrencia ocorrencia)
	{
		if (Boolean.parseBoolean(appConfig.getElasticSearchInsert()))
		{
			esDAO.substituir(ocorrencia);
		}
	}
	
	private void esAdicionar(Ocorrencia ocorrencia)
	{
		if (Boolean.parseBoolean(appConfig.getElasticSearchInsert()))
		{
			esDAO.adicionar(ocorrencia);
		}
		
	}
	
	private Ocorrencia esConsultar(String id)
	{
		Ocorrencia ocorrencia = null;
		if (Boolean.parseBoolean(appConfig.getElasticSearchSelect()))
		{
			ocorrencia = esDAO.consultar(id);
		}
		return ocorrencia;
	}
	
	private List<String> esListarAnos()
	{
		List<String> anos = new ArrayList<>();
		if (Boolean.parseBoolean(appConfig.getElasticSearchSelect()))
		{
			anos = esDAO.listarAnos();
		}
		return anos;
	}
	
	private Map<String, String> esListarDelegacias()
	{
		Map<String, String> delegacias = new HashMap<>();
		if (Boolean.parseBoolean(appConfig.getElasticSearchSelect()))
		{
			delegacias = esDAO.listarDelegacias();
		}
		return delegacias;
	}
	
	private List<Ocorrencia> esPesquisarDinamicoLazy(List<FiltroCondicao> filtroCondicoes, int primeiroRegistro,
			int registrosPorPagina) {
		List<Ocorrencia> ocorrencias = null;
		if (Boolean.parseBoolean(appConfig.getElasticSearchSelect()))
		{
			ocorrencias = esDAO.pesquisarDinamicoLazy(filtroCondicoes, primeiroRegistro, registrosPorPagina);
		}
		return ocorrencias;
	}
}
