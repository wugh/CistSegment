package com.nlp.tree;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 * 实现前缀树
 */
public class TrieTree {
    /**
     * 树的节点结构
     */
    private class TrieNode {
        char letter;
        LinkedList<TrieNode> links;
        boolean fullWord;

        TrieNode(char letter, boolean fullWord) {
            this.fullWord = fullWord;
            this.letter = letter;
            this.links = new LinkedList<TrieNode>();
        }

        public TrieNode searchChildNode(char c) {
            int lowerBound = 0;
            int upperBound = links.size() - 1;
            while (lowerBound <= upperBound) {
                int mid = (upperBound + lowerBound) / 2;
                if (links.get(mid).letter > c)
                    upperBound = mid - 1;
                else if (links.get(mid).letter < c)
                    lowerBound = mid + 1;
                else
                    return links.get(mid);
            }
            return null;
        }

        public void addChildNode(TrieNode childNode) {
            if (links.size() == 0) {
                links.add(childNode);
            }
            else {
                links.add(childNode);
                int i = links.size() - 2;
                for (; i >= 0; i--) {
                    if (links.get(i).letter > childNode.letter) {
                        links.set(i + 1, links.get(i));
                    } else
                        break;
                }
                links.set(i + 1, childNode);
            }
        }
    }

    TrieNode root = new TrieNode('\0', false);

    public void add(String str) {
        TrieNode curNode = root;
        for (int i = 0; i < str.length(); i++) {
            char curLetter = str.charAt(i);
            TrieNode childNode = curNode.searchChildNode(curLetter);
            if (childNode != null) {
                curNode = childNode;
                if (i == str.length() - 1) {
                    curNode.fullWord = true;
                }
            }
            else {
                if (i == str.length() - 1)
                    childNode = new TrieNode(curLetter, true);
                else
                    childNode = new TrieNode(curLetter, false);
                curNode.addChildNode(childNode);
                curNode = childNode;
            }
        }
    }

    public boolean find(String str) {
        TrieNode curNode = root;
        for (int i = 0; i < str.length(); i++) {
            char curLetter = str.charAt(i);
            TrieNode childNode = curNode.searchChildNode(curLetter);
            if (childNode == null)
                return false;
            else {
                curNode = childNode;
            }
        }

        return curNode.fullWord;
    }

    private void printTree(TrieNode curNode, int level, char[] branch) {
        if (curNode.links.size() != 0) {
            // 对于非叶子节点
            for (int i = 0; i < curNode.links.size(); i++) {
                branch[level] = curNode.letter;
                printTree(curNode.links.get(i), level + 1, branch);
            }
        } else {
            // 对于叶子节点
            branch[level] = curNode.letter;
        }

        if (curNode.fullWord)
        {
            for (int j = 1; j <= level; j++)
                System.out.print(branch[j]);
            System.out.println();
        }
    }

    public void printTree() {
        char[] branch = new char[50];
        printTree(this.root, 0, branch);
    }

    public static void main(String[] args) throws IOException {
        long startTime = System.nanoTime();

        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("corpus/webdict/webdict_with_freq.txt"), "UTF-8"));
        TrieTree trieTree = new TrieTree();
        ArrayList<String> words = new ArrayList<String>();

        System.out.println("start build");
        String line;
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (!line.isEmpty()) {
                String[] items = line.split("\\s+");
                trieTree.add(items[0]);
                words.add(items[0]);
            }
        }
        long endTime = System.nanoTime();
        long duration = endTime - startTime;
        System.out.println(duration);
        startTime = endTime;

        System.out.println("start find");
        for (String word: words) {
            if (trieTree.find(word) == false) {
                System.out.println(word);
            }
        }

        endTime = System.nanoTime();
        duration = endTime - startTime;
        System.out.println(duration);


    }
}

