package br.com.fences.deicdivecarbackend.roubocarga.dao;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.bson.Document;

import br.com.fences.deicdivecarbackend.config.AppConfig;
import br.com.fences.fencesutils.verificador.Verificador;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;

@ApplicationScoped
public class MongoProvider {  

	private static final String COLECAO_ROUBO_CARGA = "rdo_roubo_carga_recep05"; 
	//private static final String COLECAO_ROUBO_CARGA = "rdo_roubo_carga_recep04"; 
	//private static final String COLECAO_ROUBO_CARGA = "rdo_roubo_carga_recep03";
	//private static final String COLECAO_ENDERECO_AVULSO = "endereco_avulso";
	private static final String COLECAO_ENDERECO_AVULSO = "endereco_avulso02";
	
	private MongoClient conexao;
	private MongoDatabase banco;
	private MongoCollection<Document> colecaoRdoRouboCarga;
	private MongoCollection<Document> colecaoEnderecosAvulsos;
	
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
		
		colecaoEnderecosAvulsos = banco.getCollection(COLECAO_ENDERECO_AVULSO);
		if (colecaoEnderecosAvulsos == null)
		{
			banco.createCollection(COLECAO_ENDERECO_AVULSO);
			colecaoEnderecosAvulsos = banco.getCollection(COLECAO_ENDERECO_AVULSO);
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
}
