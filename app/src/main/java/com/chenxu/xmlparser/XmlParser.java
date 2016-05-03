package com.chenxu.xmlparser;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * Created by chenxu on 2016/5/3.
 */
public class XmlParser {
    public enum TokenType {
        XML_HINT, XML_COMMENT, XML_START_TAG_START_SIGN, XML_START_TAG, XML_START_TAG_END_SIGN, XML_ATTR_KEY, XML_ATTR_SEPARATOR,
        XML_ATTR_VALUE, XML_END_TAG_START_SIGN, XML_END_TAG, XML_END_TAG_END_SIGN, XML_TEXT, XML_CDATA, XML_END
    }

    ;
    private static final String CDATA_START = "<![CDATA[";
    private static final String CDATA_END = "]]>";

    private int parserIndex = 0;
    private String xmlString;
    private Stack<TokenType> typeStack;
    private Stack<Object> objectStack;
    private ResultBean rootBean;
    private String fileNameInAssetsFolder;
    private Context context;
    private String filePath;

    public XmlParser(Context context, String xmlString) {
        this.context = context;
        this.xmlString = xmlString;

        typeStack = new Stack<>();
        objectStack = new Stack<>();
    }


    public ResultBean xmlStringToObject() {
        parserIndex = 0;
        typeStack = new Stack<TokenType>();
        objectStack = new Stack<Object>();

        for (;;) {
            TokenType type = getNextToken();
            if (type == TokenType.XML_END) {
                break;
            }
        }
        if (!objectStack.isEmpty()) {
            ResultBean result = (ResultBean) objectStack.peek();
            Log.i("chenxu", "parsed object:" + result);
            return result;
        } else {
            return null;
        }
    }

    public String objectToXmlString() {
        if (rootBean!=null) {
            String result = rootBean.toXmlString();
            Log.i("chenxu", "to xml string:"+result);
            return result;
        } else {
            return "";
        }
    }
    public void setFileNameInAssetsFolder(String fileNameInAssetsFolder) {
        this.fileNameInAssetsFolder = fileNameInAssetsFolder;
        try {
            InputStream inputStream = null;
            inputStream = context.getResources().getAssets()
                    .open(this.fileNameInAssetsFolder);
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    inputStream, "UTF-8"));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append(" ");
            }
            this.xmlString = stringBuilder.toString();
            reader.close();
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(filePath);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            StringBuilder stringBuilder = new StringBuilder();
            String line = "";
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append(" ");
            }
            this.xmlString = stringBuilder.toString();
            reader.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    public TokenType getNextToken() {
        TokenType type = getNextTokenType();
        switch (type) {
            case XML_HINT: {
                parserIndex += 2;
                while (isIndexValid(parserIndex)) {
                    char c = xmlString.charAt(parserIndex);
                    if (c == '"') {
                        int nextQuoteIndex = xmlString
                                .indexOf('"', parserIndex + 1);
                        if (nextQuoteIndex == -1) {
                            dumpError();
                        } else {
                            parserIndex = nextQuoteIndex + 1;
                        }
                    } else if (c == '?') {
                        if (isIndexValid(parserIndex + 1)
                                && xmlString.charAt(parserIndex + 1) == '>') {
                            parserIndex += 2;
                            break;
                        }
                    } else {
                        parserIndex++;
                    }
                }
            }
            break;
            case XML_START_TAG_START_SIGN: {
                ++parserIndex;
                typeStack.push(type);
            }
            break;
            case XML_START_TAG: {
                String startTagName = getTag();
                if (typeStack.size() >= 1
                        && typeStack.get(typeStack.size() - 1) == TokenType.XML_START_TAG_START_SIGN) {
                    if (objectStack.size() <= 0) {
                        rootBean = new ResultBean();
                        rootBean.setName(startTagName);
                        objectStack.push(rootBean);

                    } else {
                        ResultBean bean = new ResultBean();
                        bean.setName(startTagName);
                        objectStack.push(bean);

                    }
                }
                typeStack.push(type);

            }
            break;
            case XML_START_TAG_END_SIGN: {
                ++parserIndex;
                typeStack.push(type);
            }
            break;
            case XML_END_TAG_START_SIGN: {
                parserIndex += 2;
                typeStack.push(type);
            }
            break;
            case XML_END_TAG: {
                String endTagName = getTag();
                if (objectStack.size()>0){
                    ResultBean lastBean = (ResultBean) objectStack.peek();
                    if (lastBean.getName().equals(endTagName)){
                        typeStack.push(type);

                    } else {
                        dumpError();
                    }
                } else {
                    dumpError();
                }
            }
            break;
            case XML_END_TAG_END_SIGN: {
                if (typeStack.size() > 0) {
                    TokenType topType = typeStack.peek();
                    if (topType == TokenType.XML_END_TAG) {
                        parserIndex++;
                    } else {
                        parserIndex += 2;
                    }
                } else {
                    dumpError();
                }
                if (objectStack.size() >= 2) {
                    ResultBean child = (ResultBean) objectStack.pop();
                    ResultBean parentBean = (ResultBean) objectStack.peek();
                    parentBean.addChild(child);
                } else {
                }
                TokenType t = TokenType.XML_END;
                do {
                    if (typeStack.size() > 0) {
                        t = typeStack.peek();
                        if (t != TokenType.XML_START_TAG_START_SIGN) {
                            typeStack.pop();
                        }
                        t = typeStack.peek();
                    } else {
                        break;
                    }
                } while (t != TokenType.XML_START_TAG_START_SIGN);
            }
            break;
            case XML_ATTR_KEY: {
                String key = getAttributeKey();
                String separator = getAttributeSeparator();
                String value = getAttributeValue();
                if (objectStack.size() > 0) {
                    ResultBean bean = (ResultBean) objectStack.peek();
                    bean.setAttribute(key, value);
                } else {
                    dumpError();
                }
            }
            break;
            case XML_ATTR_SEPARATOR: {

            }
            break;
            case XML_ATTR_VALUE: {

            }
            break;
            case XML_TEXT: {
                String text = getText();
                if (objectStack.size() > 0) {
                    ResultBean bean = (ResultBean) objectStack.peek();
                    bean.setText(text);
                }
            }
            break;
            case XML_END:
                break;
            default:
                break;
        }
        return type;
    }
    public String getTag() {
        skipWhitespace();
        int beginIndex = parserIndex;
        while (isIndexValid(parserIndex)) {
            char c = xmlString.charAt(parserIndex);
            if (c == '>' || c == '/' || Character.isWhitespace(c)) {
                return xmlString.substring(beginIndex, parserIndex);
            } else {
                parserIndex++;
            }
        }
        return null;
    }

    public String getAttributeKey() {
        skipWhitespace();
        int beginIndex = parserIndex;
        while (isIndexValid(parserIndex)) {
            char c = xmlString.charAt(parserIndex);
            if (c == '=') {
                return xmlString.substring(beginIndex, parserIndex);
            } else {
                parserIndex++;
            }
        }
        return null;

    }

    public String getAttributeSeparator() {
        skipWhitespace();
        int equalSignIndex = xmlString.indexOf('=', parserIndex);
        if (equalSignIndex == -1) {
            dumpError();
        } else {
            parserIndex = equalSignIndex + 1;
        }
        return "=";
    }

    public String getAttributeValue() {
        skipWhitespace();
        if (xmlString.charAt(parserIndex) != '"') {
            dumpError();
        }
        int beginIndex = parserIndex + 1;
        int indexOfEndDoubleQuote = xmlString.indexOf('"', beginIndex);
        if (indexOfEndDoubleQuote == -1) {
            dumpError();
            return null;
        } else {
            parserIndex = indexOfEndDoubleQuote + 1;
            return xmlString.substring(beginIndex, indexOfEndDoubleQuote);
        }
    }

    public String getText() {
        skipWhitespace();
        int beginIndex = parserIndex;
        int indexOfLeftBracket;
        while (isIndexValid(parserIndex)) {
            indexOfLeftBracket = xmlString.indexOf('<', parserIndex);
            if (indexOfLeftBracket == -1) {
                throw new RuntimeException("< expected");
            } else if (isIndexValid(parserIndex + 8)) {
                if (CDATA_START.equals(xmlString.substring(indexOfLeftBracket,
                        indexOfLeftBracket + CDATA_START.length()))) {
                    int indexOfCDATAEnd = xmlString.indexOf(CDATA_END,
                            indexOfLeftBracket + CDATA_START.length());
                    parserIndex = indexOfCDATAEnd + CDATA_END.length();
                } else {
                    parserIndex = indexOfLeftBracket;
                    break;
                }
            } else {
                parserIndex = indexOfLeftBracket;
                break;
            }
        }
        String result = null;
        result = xmlString.substring(beginIndex, parserIndex);
        result = result.trim();
        result = processCDATAAndEntityReference(result);
        return result;
    }

    public String replaceEntityReference(String s) {
        String result = s.replace("&lt;", "<");
        result = result.replace("&gt;", ">");
        result = result.replace("&amp;", "&");
        result = result.replace("&apos", "'");
        result = result.replace("&quot", "\"");
        return result;
    }

    public String processCDATAAndEntityReference(String s) {
        if (TextUtils.isEmpty(s)) {
            return s;
        } else if (isCDATA(s)) {
            s = s.replace(CDATA_START, "");
            s = s.replace(CDATA_END, "");
        } else {
            s = replaceEntityReference(s);
        }
        return s;
    }

    public boolean isCDATA(String s) {
        if (TextUtils.isEmpty(s)) {
            return false;
        } else {
            s = s.trim();
            return s.startsWith(CDATA_START) && s.endsWith(CDATA_END);
        }
    }
    public TokenType getNextTokenType() {
        TokenType currTokenType = TokenType.XML_END;
        TokenType nextTokenType = TokenType.XML_END;
        if (typeStack.size() > 0) {
            currTokenType = typeStack.peek();
        }
        skipWhitespace();
        if (isIndexValid(parserIndex)) {
            char c = xmlString.charAt(parserIndex);
            if (c == '<') {
                if (isIndexValid(parserIndex + 1)) {
                    char ch = xmlString.charAt(parserIndex + 1);
                    if (ch == '?') {
                        nextTokenType = TokenType.XML_HINT;
                    } else if (ch == '!') {
                        if (isIndexValid(parserIndex + 3) && xmlString.charAt(parserIndex + 2) == '-' && xmlString.charAt(parserIndex + 3) == '-') {
                            nextTokenType = TokenType.XML_COMMENT;
                        } else if (isIndexValid(parserIndex + 8)
                                && CDATA_START.equals(xmlString.substring(
                                parserIndex,
                                parserIndex + CDATA_START.length()))
                                && currTokenType == TokenType.XML_START_TAG_END_SIGN) {
                            nextTokenType = TokenType.XML_CDATA;
                        } else {
                            dumpError();
                        }
                    } else if (ch == '/') {
                        nextTokenType = TokenType.XML_END_TAG_START_SIGN;
                    } else if (isCharacterTagPart(ch)) {
                        nextTokenType = TokenType.XML_START_TAG_START_SIGN;
                    } else {
                        dumpError();
                    }
                } else {
                    dumpError();
                }
            } else if (c == '>') {
                if (currTokenType == TokenType.XML_START_TAG) {
                    nextTokenType = TokenType.XML_START_TAG_END_SIGN;
                } else if (currTokenType == TokenType.XML_ATTR_VALUE) {
                    nextTokenType = TokenType.XML_START_TAG_END_SIGN;
                } else if (currTokenType == TokenType.XML_END_TAG) {
                    nextTokenType = TokenType.XML_END_TAG_END_SIGN;
                } else {
                    dumpError();
                }

            }else if (c == '/') {
                if (isIndexValid(parserIndex + 1)) {
                    char nextChar = xmlString.charAt(parserIndex + 1);
                    if (nextChar == '>') {
                        nextTokenType = TokenType.XML_END_TAG_END_SIGN;
                    } else {
                        dumpError();
                    }
                } else {
                    dumpError();
                }
            } else if (isCharacterTagValuePart(c)) {
               if (currTokenType == TokenType.XML_START_TAG_START_SIGN) {
                    nextTokenType = TokenType.XML_START_TAG;
                } else if (currTokenType == TokenType.XML_START_TAG) {
                    nextTokenType = TokenType.XML_ATTR_KEY;
                } else if (currTokenType == TokenType.XML_START_TAG_END_SIGN) {
                    nextTokenType = TokenType.XML_TEXT;
                } else if (currTokenType == TokenType.XML_END_TAG_START_SIGN) {
                    nextTokenType = TokenType.XML_END_TAG;
                } else {
                    dumpError();
                }
            } else {
                dumpError();
            }
        }
        return nextTokenType;
    }

    public TokenType getCurrentTokenType(){
        TokenType currTokenType = TokenType.XML_END;
        if (typeStack.size() > 0) {
            currTokenType = typeStack.peek();
        }
        return currTokenType;
    }

    public boolean isCharacterTagPart(char c) {
        return isCharacterAlphabetic(c) || (c == '_');
    }

    public boolean isCharacterTagValuePart(char c) {
        return (c != '<') && (c != '&');
    }

    public boolean isCharacterAlphabetic(char c) {
        String s = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdedfhijklmnopqrstuvwxyz";
        int index = s.indexOf(c);
        return index != -1;
    }

    private void dumpError() {
        throw new RuntimeException("xml string error at index :" + parserIndex);
    }

    private boolean isIndexValid(int index) {
        return index >= 0 && index < xmlString.length();
    }

    private void skipWhitespace() {
        while (parserIndex < xmlString.length()) {
            char c = xmlString.charAt(parserIndex);
            if (Character.isWhitespace(c)) {
                ++parserIndex;
            } else {
                break;
            }
        }
    }

    public class ResultBean {
        private String name;
        private Map<String, String> map;
        private String text;
        private List<ResultBean> list;

        public ResultBean() {
            super();
            name = null;
            map = new HashMap<String, String>();
            text = null;
            list = new ArrayList<ResultBean>();
        }

        public ResultBean(String name, Map<String, String> map, String text,
                          List<ResultBean> list) {
            super();
            this.name = name;
            this.map = map;
            this.text = text;
            this.list = list;
        }

        public String toXmlString() {
            StringBuilder builder = new StringBuilder();
            builder.append("<");
            builder.append(name + " ");
            if (map != null) {
                for (Map.Entry<String, String> entry : map.entrySet()) {
                    builder.append(entry.getKey());
                    builder.append("=");
                    builder.append("\"" + entry.getValue() + "\"" + " ");
                }
            }
            builder.append(">");
            if (text != null) {
                if (text.contains("<") || text.contains("&")) {
                    builder.append(CDATA_START);
                    builder.append(text);
                    builder.append(CDATA_END);
                } else {
                    builder.append(text);
                }
            }
            if (list != null) {
                for (int i = 0; i < list.size(); i++) {
                    ResultBean subBean = list.get(i);
                    builder.append(subBean.toXmlString());
                }
            }
            builder.append("</" + name + ">");
            return builder.toString();
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append(name + " ");
            builder.append(map + " ");
            builder.append(list + " ");
            builder.append(text + "\n");
            return builder.toString();
        }

        public String getAttribute(String key) {
            if (map != null) {
                return map.get(key);
            }
            return null;
        }

        public void setAttribute(String key, String value) {
            if (map == null) {
                map = new HashMap<String, String>();
            }
            map.put(key, value);
        }

        public void addChild(ResultBean child) {
            if (list == null) {
                list = new ArrayList<ResultBean>();
            }
            list.add(child);
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Map<String, String> getMap() {
            return map;
        }

        public void setMap(Map<String, String> map) {
            this.map = map;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public List<ResultBean> getList() {
            return list;
        }

        public void setList(List<ResultBean> list) {
            this.list = list;
        }
    }

}
