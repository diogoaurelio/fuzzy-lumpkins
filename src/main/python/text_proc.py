
import spacy

nlp = spacy.load('pt_core_news_sm')


sample = """Este T2 (apartamento com 3 assoalhadas) sito na Rua José Maria Nicolau, em São Domingos de Benfica, junto ao Estádio da Luz dispõe de Garagem e Arrecadação.
Dos 2 quartos, 1 deles é suite. Dispõe de 2 casas de banho completos, Cozinha com a zona de lavandaria separada, Sala generosa e varanda.
Prédio de 2005 com obras recentes e condomínio cuidado.
A sua excelente localização permite acessos rápidos e fáceis às principais artérias de Lisboa, como a 2ª Circular e Eixo Norte-Sul, bem como acesso pedonal a menos de 1Km de distância do Metro das Estações Alto dos Moinhos ou Colégio Militar / Luz, Centro Comercial Colombo, Estádio da Luz (Sport Lisboa e Benfica), Pingo Doce e outros supermercados, Escolas e Colégios Privados diversos.
O apartamento actualmente tem inquilino que, de acordo com a vontade do proponente comprador, poderá mantê-lo ou rescindir o contrato antes da venda."""

# POS (parts of speech) tagging
doc = nlp(sample)

# entity recognition
for ent in doc.ents:
    print(ent.text, ent.start_char, ent.end_char, ent.label_)