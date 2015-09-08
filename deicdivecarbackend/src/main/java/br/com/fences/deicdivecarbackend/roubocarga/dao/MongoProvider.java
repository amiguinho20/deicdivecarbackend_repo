package br.com.fences.deicdivecarbackend.roubocarga.dao;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.bson.Document;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;

import br.com.fences.deicdivecarbackend.config.AppConfig;
import br.com.fences.fencesutils.verificador.Verificador;

@ApplicationScoped
public class MongoProvider {  

	private static final String COLECAO_ROUBO_CARGA = "rdo_roubo_carga_recep06";   
	//private static final String COLECAO_ROUBO_CARGA = "rdo_roubo_carga_recep04"; 
	//private static final String COLECAO_ROUBO_CARGA = "rdo_roubo_carga_recep03";
	//private static final String COLECAO_ENDERECO_AVULSO = "endereco_avulso";
	private static final String COLECAO_ENDERECO_AVULSO = "endereco_avulso02";
	
	private static final String COLECAO_CONTROLE_OCORRENCIA = "controle_ocorrencia";
	
	private static final String COLECAO_INDICIADO = "indiciado";
	
	private MongoClient conexao;
	private MongoDatabase banco;
	private MongoCollection<Document> colecaoRdoRouboCarga;
	private MongoCollection<Document> colecaoEnderecosAvulsos;
	private MongoCollection<Document> colecaoControleOcorrencia;
	private MongoCollection<Document> colecaoIndiciado;
	
	@Inject
	private AppConfig appConfig;
	
	@PostConstruct
	public void abrirConexao() 
	{
		String dbMongoHost = appConfig.getDbMongoHost();
		String dbMongoPort = appConfig.getDbMongoPort();
		String dbMongoDatabase = appConfig.getDbMongoDatabase();
		String dbMongoUser = appConfig.getDbMongoUser();
		String dbMongoPass = appConfig.getDbMongoPass();
		
		if (Verificador.isValorado(dbMongoUser))
		{
			String uriConexao = String.format("mongodb://%s:%s@%s:%s/%s", dbMongoUser, dbMongoPass, dbMongoHost, dbMongoPort, dbMongoDatabase);
			MongoClientURI uri  = new MongoClientURI(uriConexao); 
			conexao = new MongoClient(uri);
		}
		else
		{
			conexao = new MongoClient(dbMongoHost, Integer.parseInt(dbMongoPort));
		}
		banco = conexao.getDatabase(dbMongoDatabase);
		

		//		colecao = banco.getCollection(COLECAO);
		colecaoRdoRouboCarga = banco.getCollection(COLECAO_ROUBO_CARGA);
		if (colecaoRdoRouboCarga == null)
		{
			banco.createCollection(COLECAO_ROUBO_CARGA);
			colecaoRdoRouboCarga = banco.getCollection(COLECAO_ROUBO_CARGA);
			   
			BasicDBObject campos = new BasicDBObject();
			campos.append("DATAHORA_REGISTRO_BO", 1);
			
			IndexOptions opcoes =  new IndexOptions();
			opcoes.unique(true);
			
			colecaoRdoRouboCarga.createIndex(campos, opcoes);
			
			BasicDBObject campos2 = new BasicDBObject();
			campos2.append("ANO_BO", 1);
			campos2.append("NUM_BO", 1);
			campos2.append("ID_DELEGACIA", 1);
			
			colecaoRdoRouboCarga.createIndex(campos2, opcoes);
		}
/*		{
			MongoCursor<Document> indicesIterator = colecaoRdoRouboCarga.listIndexes().iterator();
			final String INDICE01 = "datahoraRegistroBo";
			final String INDICE02 = "anoBoNumBoIdDelegacia";
			boolean indice01 = false;
			boolean indice02 = false;

			try {
				while (indicesIterator.hasNext()) {
					Document indice = indicesIterator.next();
					String nome = indice.getString("name");

					if (INDICE01.equalsIgnoreCase(nome)) {
						indice01 = true;
					}
					if (INDICE02.equalsIgnoreCase(nome)) {
						indice02 = true;
					}
				}
			} finally {
				indicesIterator.close();

				if (!indice01) {
					BasicDBObject campos = new BasicDBObject();
					campos.append("DATAHORA_REGISTRO_BO", 1);

					IndexOptions opcoes = new IndexOptions();
					opcoes.unique(true);
					opcoes.name(INDICE01);

					colecaoControleOcorrencia.createIndex(campos, opcoes);
				}
				if (!indice02) {
					BasicDBObject campos2 = new BasicDBObject();
					campos2.append("ANO_BO", 1);
					campos2.append("NUM_BO", 1);
					campos2.append("ID_DELEGACIA", 1);

					IndexOptions opcoes = new IndexOptions();
					opcoes.unique(true);
					opcoes.name(INDICE02);

					colecaoControleOcorrencia.createIndex(campos2, opcoes);
				}
			}
		}
*/		
		
		colecaoControleOcorrencia = banco.getCollection(COLECAO_CONTROLE_OCORRENCIA);
		if (colecaoControleOcorrencia == null)
		{
			banco.createCollection(COLECAO_CONTROLE_OCORRENCIA);
			colecaoControleOcorrencia = banco.getCollection(COLECAO_CONTROLE_OCORRENCIA);
		}	
/*
		MongoCursor<Document> indicesIterator = colecaoControleOcorrencia.listIndexes().iterator();
		final String INDICE01 = "datahoraRegistroBo";
		final String INDICE02 = "anoBoNumBoIdDelegacia";
		boolean indice01 = false;
		boolean indice02 = false;
		
		try {
			while (indicesIterator.hasNext()) 
			{
				Document indice = indicesIterator.next();
				String nome = indice.getString("name");
				
				if (INDICE01.equalsIgnoreCase(nome))
				{
					indice01 = true;
				}
				if (INDICE02.equalsIgnoreCase(nome))
				{
					indice02 = true;
				}
			}
		} finally {
			indicesIterator.close();
			
			if (!indice01)
			{
				BasicDBObject campos = new BasicDBObject();
				campos.append("datahoraRegistroBo", 1);

				IndexOptions opcoes = new IndexOptions();
				opcoes.unique(true);
				opcoes.name(INDICE01);

				colecaoControleOcorrencia.createIndex(campos, opcoes);
			}
			if (!indice02)
			{
				BasicDBObject campos2 = new BasicDBObject();
				campos2.append("anoBo", 1);
				campos2.append("numBo", 1);
				campos2.append("idDelegacia", 1);

				IndexOptions opcoes = new IndexOptions();
				opcoes.unique(true);
				opcoes.name(INDICE02);

				colecaoControleOcorrencia.createIndex(campos2, opcoes);
			}
		}
*/		
		colecaoEnderecosAvulsos = banco.getCollection(COLECAO_ENDERECO_AVULSO);
		if (colecaoEnderecosAvulsos == null)
		{
			banco.createCollection(COLECAO_ENDERECO_AVULSO);
			colecaoEnderecosAvulsos = banco.getCollection(COLECAO_ENDERECO_AVULSO);
		}
		
		colecaoIndiciado = banco.getCollection(COLECAO_INDICIADO);
		if (colecaoIndiciado == null)
		{
			banco.createCollection(COLECAO_INDICIADO);
			colecaoIndiciado = banco.getCollection(COLECAO_INDICIADO);
		}
		
	}
	
	/**
	 * Fechar a conexao com o banco quando o objeto for destruido.
	 */
	@PreDestroy
	public void fecharConecao()
	{
		conexao.close();
	}
	
	@Produces @ColecaoRouboCarga
	public MongoCollection<Document> getColecaoRouboCarga()
	{
		return colecaoRdoRouboCarga;
	}
	
	@Produces @ColecaoEnderecoAvulso
	public MongoCollection<Document> getColecaoEnderecoAvulso()
	{
		return colecaoEnderecosAvulsos;
	}

	@Produces @ColecaoControleOcorrencia
	public MongoCollection<Document> getColecaoControleOcorrencia()
	{
		return colecaoControleOcorrencia;
	}
	
	@Produces @ColecaoIndiciado
	public MongoCollection<Document> getColecaoIndiciado()
	{
		return colecaoIndiciado;
	}

}
