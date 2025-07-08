package com.gameadvisor.client.ui.components;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class GameAdvisorClient {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Gemini API 테스트");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 200);

        JButton button = new JButton("Gemini 테스트 요청");
        JLabel label = new JLabel("결과가 여기에 표시됩니다.", SwingConstants.CENTER);

        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    URL url = new URL("http://localhost:8080/api/gemini-test");
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    con.setRequestMethod("GET");
                    BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"));
                    String inputLine;
                    StringBuilder content = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        content.append(inputLine);
                    }
                    in.close();
                    label.setText(content.toString());
                } catch (Exception ex) {
                    label.setText("에러 발생: " + ex.getMessage());
                }
            }
        });

        frame.setLayout(new BorderLayout());
        frame.add(button, BorderLayout.NORTH);
        frame.add(label, BorderLayout.CENTER);
        frame.setVisible(true);
    }
} 