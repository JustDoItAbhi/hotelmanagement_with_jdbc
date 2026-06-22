package com.hotel_service.city_country.service;

import lombok.val;

import java.util.HashMap;
import java.util.Map;

public class Trie {
    boolean endOfWord=false;
    Map<Character,Trie>children=new HashMap<>();

    public Trie() {
        this.endOfWord = endOfWord;
        this.children = children;
    }
    public  void insert(String  word) {
        Trie  node = this;
        for (int i = 0; i < word.length(); i++) {
            char ch = word.charAt(i);
           Trie nextNode=node.children.get(ch);
           if(nextNode==null){
               nextNode=new Trie();
               node.children.put(ch,nextNode);
           }
           node=nextNode;

        }
        node.endOfWord=true;
    }
    public boolean search(String word){
        Trie currentNode=this;
        for(int i=0;i< word.length();i++){
            char ch= word.charAt(i);
            Trie nextNode=currentNode.children.get(ch);
            if(nextNode==null){
                return false;
            }
            currentNode=nextNode;
        }
        return currentNode.endOfWord;
    }
    public boolean startsWith(String prifix){
        Trie currentNode=this;
        for(int i=0;i< prifix.length();i++){
            char ch= prifix.charAt(i);
            Trie nextNode=currentNode.children.get(ch);
            if(nextNode==null){
                return false;
            }
            currentNode=nextNode;
        }
        return true;
    }
    public java.util.List<String> startsWithWords(String prefix) {
        java.util.List<String> results = new java.util.ArrayList<>();

        Trie currentNode = this;

        for (int i = 0; i < prefix.length(); i++) {
            char ch = prefix.charAt(i);

            Trie nextNode = currentNode.children.get(ch);
            if (nextNode == null) {
                return results;
            }

            currentNode = nextNode;
        }

        collectWords(currentNode, prefix, results);
        return results;
    }

    private void collectWords(Trie node, String prefix, java.util.List<String> results) {

        if (node.endOfWord) {
            results.add(prefix);
        }

        for (Map.Entry<Character, Trie> entry : node.children.entrySet()) {
            char ch = entry.getKey();
            Trie childNode = entry.getValue();

            collectWords(childNode, prefix + ch, results);
        }
    }

    public boolean delete(String word) {
        return deleteHelper(this, word, 0);
    }

    private boolean deleteHelper(Trie node, String word, int index) {
        if (index == word.length()) {

            if (!node.endOfWord) {
                return false;
            }

            node.endOfWord = false;

            return node.children.isEmpty();
        }

        char ch = word.charAt(index);
        Trie childNode = node.children.get(ch);

        if (childNode == null) {
            return false;
        }

        boolean shouldDeleteChild = deleteHelper(childNode, word, index + 1);

        if (shouldDeleteChild) {
            node.children.remove(ch);
            return node.children.isEmpty() && !node.endOfWord;
        }

        return false;
    }
}
