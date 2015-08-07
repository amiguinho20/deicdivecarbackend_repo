package br.com.fences.deicdivecarbackend.roubocarga.converter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import org.bson.Document;
import org.bson.types.ObjectId;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import br.com.fences.fencesutils.conversor.mongodb.Converter;
import br.com.fences.fencesutils.verificador.Verificador;
import br.com.fences.ocorrenciaentidade.ocorrencia.Ocorrencia;
import br.com.fences.ocorrenciaentidade.ocorrencia.auxiliar.Auxiliar;

@ApplicationScoped
public class OcorrenciaConverter extends Converter<Ocorrencia>{

	private Gson gson = new GsonBuilder()
			.registerTypeHierarchyAdapter(Collection.class, new ColecaoJsonAdapter())
			.create();
	
	@Override
	public Document paraDocumento(Ocorrencia ocorrencia) 
	{
    	String json = gson.toJson(ocorrencia);
    	String jsonMongoDB = transformarIdParaJsonDb(json);
    	Document documento = Document.parse(jsonMongoDB);
    	
    	//-- Referencia transiente, apenas coloca o ID no atributo
    	if (ocorrencia.getAuxiliar() != null)
    	{
    		Auxiliar auxiliar = ocorrencia.getAuxiliar();
    		if (Verificador.isValorado(auxiliar.getFilhos()))
    		{
        		List<ObjectId> idFilhos = new ArrayList<>();
        		for (Ocorrencia filho : auxiliar.getFilhos())
        		{
        			String idFilho = filho.getId();
        			ObjectId idObj = new ObjectId(idFilho);
        			idFilhos.add(idObj);
        		}
        		
        		if (documento.get("AUXILIAR") != null)
        		{
        			Document docAuxiliar = (Document) documento.get("AUXILIAR");
        			docAuxiliar.append("FILHOS", idFilhos);
        		}
        		else
        		{
        			documento.append("AUXILIAR", new Document("FILHOS", idFilhos));
        		}
    		}
    		if (auxiliar.getPai() != null)
    		{
    			Ocorrencia pai = auxiliar.getPai();
    			String idPai = pai.getId();
    			ObjectId idObj = new ObjectId(idPai);
    			
        		if (documento.get("AUXILIAR") != null)
        		{
        			Document docAuxiliar = (Document) documento.get("AUXILIAR");
        			docAuxiliar.append("PAI", idObj);
        		}
        		else
        		{
        			documento.append("AUXILIAR", new Document("PAI", idObj));
        		}
    		}
    	}
 		return documento;
	}

	@Override
	public Ocorrencia paraObjeto(Document doc) {
		
		String jsonMongoDB = doc.toJson();
    	String json = transformarIdParaJsonObj(jsonMongoDB);
    	Ocorrencia ocorrencia = gson.fromJson(json, Ocorrencia.class);
    	
    	//-- Referencia transiente, apenas coloca o ID no atributo
    	if (doc.get("AUXILIAR") != null)
    	{
    		Document docAuxiliar = (Document) doc.get("AUXILIAR");
    		if (docAuxiliar.get("FILHOS") != null)
    		{
    			List filhosDocList = (ArrayList) doc.get("FILHOS");
    			if (Verificador.isValorado(filhosDocList))
    			{
	        		for (Object obj: filhosDocList)
	            	{
	            		ObjectId id = (ObjectId) obj;
	            		Ocorrencia filho = new Ocorrencia();
	            		filho.setId(obj.toString());
	            		ocorrencia.getAuxiliar().getFilhos().add(filho);
	            	}
    			}
    		}
    		if (docAuxiliar.get("PAI") != null)
    		{
    			ObjectId idPai = (ObjectId) docAuxiliar.get("PAI");
        		Ocorrencia pai = new Ocorrencia();
        		pai.setId(idPai.toString());
        		ocorrencia.getAuxiliar().setPai(pai);
    		}
    		
    		
    	}
    	
    	
    
    	return ocorrencia;

/*
		Ocorrencia ocorrencia = new Ocorrencia();

		ocorrencia.setId(doc.get("_id").toString());
		ocorrencia.setIdDelegacia(doc.getString("ID_DELEGACIA"));
		ocorrencia.setNomeDelegacia(doc.getString("NOME_DELEGACIA"));
		ocorrencia.setAnoBo(doc.getString("ANO_BO"));
		ocorrencia.setNumBo(doc.getString("NUM_BO"));
		ocorrencia.setDelegReferenciaBo(doc.getString("DELEG_REFERENCIA_BO"));
		ocorrencia.setNomeDelegaciaReferenciaBo(doc.getString("NOME_DELEGACIA_REFERENCIA_BO"));
		ocorrencia.setAnoReferenciaBo(doc.getString("ANO_REFERENCIA_BO"));
		ocorrencia.setNumReferenciaBo(doc.getString("NUM_REFERENCIA_BO"));
		ocorrencia.setDataOcorrenciaBo(doc.getString("DATA_OCORRENCIA_BO"));
		ocorrencia.setDatahoraRegistroBo(doc.getString("DATAHORA_REGISTRO_BO"));
		ocorrencia.setFlagFlagrante(doc.getString("FLAG_FLAGRANTE"));
		ocorrencia.setLogradouro(doc.getString("LOGRADOURO"));
		ocorrencia.setNumeroLogradouro(doc.getString("NUMERO_LOGRADOURO"));
		ocorrencia.setComplemento(doc.getString("COMPLEMENTO"));
		ocorrencia.setCep(doc.getString("CEP"));
		ocorrencia.setBairro(doc.getString("BAIRRO"));
		ocorrencia.setCidade(doc.getString("CIDADE"));
		ocorrencia.setIdUf(doc.getString("ID_UF"));
		ocorrencia.setLatitude(doc.getString("LATITUDE"));
		ocorrencia.setLongitude(doc.getString("LONGITUDE"));
		ocorrencia.setGoogleLatitude(doc.getString("GOOGLE_LATITUDE"));
		ocorrencia.setGoogleLongitude(doc.getString("GOOGLE_LONGITUDE"));
    	
    	if (doc.get("NATUREZA") != null)
    	{
    		List natDocList = (ArrayList) doc.get("NATUREZA");
    		
    		for (Object obj: natDocList)
        	{
        		Document natDoc = (Document) obj;
        		Natureza natureza = new Natureza();
        		
        		natureza.setContNatureza(natDoc.getString("CONT_NATUREZA"));
        		natureza.setIdOcorrencia(natDoc.getString("ID_OCORRENCIA"));
        		natureza.setDescrOcorrencia(natDoc.getString("DESCR_OCORRENCIA"));
        		natureza.setIdEspecie(natDoc.getString("ID_ESPECIE"));
        		natureza.setDescrEspecie(natDoc.getString("DESCR_ESPECIE"));
        		natureza.setIdSubespecie(natDoc.getString("ID_SUBESPECIE"));
        		natureza.setDescrSubespecie(natDoc.getString("DESCR_SUBESPECIE"));
        		natureza.setIdNatureza(natDoc.getString("ID_NATUREZA"));
        		natureza.setRubricaNatureza(natDoc.getString("RUBRICA_NATUREZA"));
        		natureza.setFlagStatus(natDoc.getString("FLAG_STATUS"));
        		natureza.setIdConduta(natDoc.getString("ID_CONDUTA"));
        		natureza.setDescrConduta(natDoc.getString("DESCR_CONDUTA"));
        	
        		ocorrencia.getNaturezas().add(natureza);
        	}
    	}
    	
    	if (doc.get("VEICULO") != null)
    	{
    		List veicDocList = (ArrayList) doc.get("VEICULO");
    		
    		for (Object obj: veicDocList)
    		{
           		Document veicDoc = (Document) obj;
           		Veiculo veiculo = new Veiculo();
           		
           		veiculo.setContVeiculo(veicDoc.getString("CONT_VEICULO"));
           		veiculo.setPlacaVeiculo(veicDoc.getString("PLACA_VEICULO"));
           		veiculo.setChassisVeiculo(veicDoc.getString("CHASSIS_VEICULO"));
           		veiculo.setCodRenavam(veicDoc.getString("COD_RENAVAM"));
           		veiculo.setIdTipoVeiculo(veicDoc.getString("ID_TIPO_VEICULO"));
           		veiculo.setIdCombustivel(veicDoc.getString("ID_COMBUSTIVEL"));
           		veiculo.setIdCorVeiculo(veicDoc.getString("ID_COR_VEICULO"));
           		veiculo.setIdMarcaVeiculo(veicDoc.getString("ID_MARCA_VEICULO"));
           		veiculo.setCidadeVeiculo(veicDoc.getString("CIDADE_VEICULO"));
           		veiculo.setUfVeiculo(veicDoc.getString("UF_VEICULO"));
           		veiculo.setNomeProprietarioVeiculo(veicDoc.getString("NOME_PROPRIETARIO_VEICULO"));
           		veiculo.setIdOcorrenciaVeiculo(veicDoc.getString("ID_OCORRENCIA_VEICULO"));
           		veiculo.setIdTipolocal(veicDoc.getString("ID_TIPOLOCAL"));
           		veiculo.setFlagSeguroVeiculo(veicDoc.getString("FLAG_SEGURO_VEICULO"));
           		veiculo.setSeguradoraVeiculo(veicDoc.getString("SEGURADORA_VEICULO"));
           		veiculo.setApoliceVeiculo(veicDoc.getString("APOLICE_VEICULO"));
           		veiculo.setLogotipoVeiculo(veicDoc.getString("LOGOTIPO_VEICULO"));
           		veiculo.setFlagEscoltaVeiculo(veicDoc.getString("FLAG_ESCOLTA_VEICULO"));
           		veiculo.setTempoAbandonoVeiculo(veicDoc.getString("TEMPO_ABANDONO_VEICULO"));
           		veiculo.setDocumentosLevados(veicDoc.getString("DOCUMENTOS_LEVADOS"));
           		veiculo.setAnoFabricacao(veicDoc.getString("ANO_FABRICACAO"));
           		veiculo.setAnoModelo(veicDoc.getString("ANO_MODELO"));
           		veiculo.setObservacaoVeiculo(veicDoc.getString("OBSERVACAO_VEICULO"));
           		veiculo.setChassisRemarcado(veicDoc.getString("CHASSIS_REMARCADO"));
           		veiculo.setFlagBuscaRdo(veicDoc.getString("FLAG_BUSCA_RDO"));
           		veiculo.setFlagBuscaDetran(veicDoc.getString("FLAG_BUSCA_DETRAN"));
           		veiculo.setContPessoa(veicDoc.getString("CONT_PESSOA"));
           		veiculo.setCodMunicipio(veicDoc.getString("COD_MUNICIPIO"));
           		veiculo.setFlagPlacaAntiga(veicDoc.getString("FLAG_PLACA_ANTIGA"));
           		veiculo.setDescrTipoVeiculo(veicDoc.getString("DESCR_TIPO_VEICULO"));
           		veiculo.setDescrCombustivel(veicDoc.getString("DESCR_COMBUSTIVEL"));
           		veiculo.setDescrCorVeiculo(veicDoc.getString("DESCR_COR_VEICULO"));
           		veiculo.setDescrMarcaVeiculo(veicDoc.getString("DESCR_MARCA_VEICULO"));
           		veiculo.setDescrOcorrenciaVeiculo(veicDoc.getString("DESCR_OCORRENCIA_VEICULO"));
           		veiculo.setDescrTipolocal(veicDoc.getString("DESCR_TIPOLOCAL"));
           		
           		ocorrencia.getVeiculos().add(veiculo);
    		}
    	}
    	
    	if (doc.get("CARGA") != null)
    	{
    		List cargDocList = (ArrayList) doc.get("CARGA");
    		
    		for (Object obj: cargDocList)
    		{
           		Document cargDoc = (Document) obj;
           		Carga carga = new Carga();
           		carga.setIdTipoCarga(cargDoc.getString("ID_TIPO_CARGA"));
           		carga.setIdUnidade(cargDoc.getString("ID_UNIDADE"));
           		carga.setOrigemCarga(cargDoc.getString("ORIGEM_CARGA"));
           		carga.setDestinoCarga(cargDoc.getString("DESTINO_CARGA"));
           		carga.setNotasFiscaisCarga(cargDoc.getString("NOTAS_FISCAIS_CARGA"));
           		carga.setValorCarga(cargDoc.getString("VALOR_CARGA"));
           		carga.setNomeEmbarcadorCarga(cargDoc.getString("NOME_EMBARCADOR_CARGA"));
           		carga.setLocalEmbarqueCarga(cargDoc.getString("LOCAL_EMBARQUE_CARGA"));
           		carga.setFlagCargaRecuperadaCarga(cargDoc.getString("FLAG_CARGA_RECUPERADA_CARGA"));
           		carga.setLocalRecuperacaoCarga(cargDoc.getString("LOCAL_RECUPERACAO_CARGA"));
           		carga.setDataRecuperacaoCarga(cargDoc.getString("DATA_RECUPERACAO_CARGA"));
           		carga.setHoraRecuperacaoCarga(cargDoc.getString("HORA_RECUPERACAO_CARGA"));
           		carga.setValorEstimadoRecupCarga(cargDoc.getString("VALOR_ESTIMADO_RECUP_CARGA"));
           		carga.setFlagSeguroCarga(cargDoc.getString("FLAG_SEGURO_CARGA"));
           		carga.setSerieCarga(cargDoc.getString("SERIE_CARGA"));
           		carga.setMarcaCarga(cargDoc.getString("MARCA_CARGA"));
           		carga.setIdModoObjeto(cargDoc.getString("ID_MODO_OBJETO"));
           		carga.setDetalheCarga(cargDoc.getString("DETALHE_CARGA"));
           		carga.setIdSubtipoCarga(cargDoc.getString("ID_SUBTIPO_CARGA"));
           		carga.setDescrTipoCarga(cargDoc.getString("DESCR_TIPO_CARGA"));
           		carga.setDescrModoObjeto(cargDoc.getString("DESCR_MODO_OBJETO"));
           		carga.setDescrUnidade(cargDoc.getString("DESCR_UNIDADE"));
           		
           		ocorrencia.getCargas().add(carga);
    		}
    	}    	
    	
    	if (doc.get("COMPLEMENTAR") != null)
    	{
    		List natDocList = (ArrayList) doc.get("COMPLEMENTAR");
    		
    		for (Object obj: natDocList)
        	{
        		ObjectId id = (ObjectId) obj;
        		Ocorrencia complementar = new Ocorrencia();
        		complementar.setId(obj.toString());

        		ocorrencia.getComplementares().add(complementar);
        	}
    	}
*/    	
	}	
	

	
}
