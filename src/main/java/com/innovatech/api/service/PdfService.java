package com.innovatech.api.service;

import com.innovatech.api.entity.Facture;
import com.innovatech.api.entity.LigneCommande;
import com.innovatech.api.entity.LigneCommandeAchat;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;

@Service
public class PdfService {

    public ByteArrayInputStream generateFacturePdf(Facture facture) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter writer = new PdfWriter(out);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            // Marges
            document.setMargins(40, 40, 40, 40);

            // Couleurs
            DeviceRgb primaryColor = new DeviceRgb(37, 99, 235); // Bleu
            DeviceRgb textColor = new DeviceRgb(31, 41, 55); // Gris foncé
            DeviceRgb grayColor = new DeviceRgb(107, 114, 128); // Gris moyen

            // ============================================================
            // EN-TÊTE
            // ============================================================
            Table header = new Table(UnitValue.createPercentArray(new float[]{5, 5}));
            header.setWidth(UnitValue.createPercentValue(100));

            // Côté Gauche : Infos Entreprise
            Cell leftCell = new Cell().setBorder(Border.NO_BORDER);
            leftCell.add(new Paragraph(facture.getEntreprise().getNom())
                    .setBold().setFontSize(14).setFontColor(textColor));
            if (facture.getEntreprise().getAdresse() != null)
                leftCell.add(new Paragraph(facture.getEntreprise().getAdresse()).setFontSize(9).setFontColor(grayColor));
            if (facture.getEntreprise().getVille() != null)
                leftCell.add(new Paragraph(facture.getEntreprise().getVille()).setFontSize(9).setFontColor(grayColor));
            if (facture.getEntreprise().getTelephone() != null)
                leftCell.add(new Paragraph(facture.getEntreprise().getTelephone()).setFontSize(9).setFontColor(grayColor));
            if (facture.getEntreprise().getEmail() != null)
                leftCell.add(new Paragraph(facture.getEntreprise().getEmail()).setFontSize(9).setFontColor(grayColor));
            header.addCell(leftCell);

            // Côté Droit : Titre et Dates
            Cell rightCell = new Cell().setBorder(Border.NO_BORDER);
            rightCell.add(new Paragraph("FACTURE")
                    .setBold().setFontSize(26).setFontColor(primaryColor)
                    .setTextAlignment(TextAlignment.RIGHT));
            rightCell.add(new Paragraph(facture.getNumero())
                    .setBold().setFontSize(12).setFontColor(textColor)
                    .setTextAlignment(TextAlignment.RIGHT).setMarginBottom(10));

            rightCell.add(new Paragraph("Date d'émission : " + facture.getDateEmission().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                    .setFontSize(9).setFontColor(grayColor).setTextAlignment(TextAlignment.RIGHT));
            rightCell.add(new Paragraph("Date d'échéance : " + facture.getDateEcheance().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                    .setFontSize(9).setFontColor(grayColor).setTextAlignment(TextAlignment.RIGHT));
            header.addCell(rightCell);

            document.add(header);
            document.add(new Paragraph("\n"));

            // ============================================================
            // CLIENT / FOURNISSEUR
            // ============================================================
            Table clientTable = new Table(UnitValue.createPercentArray(new float[]{5, 5}));
            clientTable.setWidth(UnitValue.createPercentValue(100));

            // Gauche
            Cell clientCell = new Cell().setBorder(Border.NO_BORDER);
            clientCell.add(new Paragraph(facture.getCommandeAchat() != null ? "FOURNISSEUR" : "FACTURÉ À")
                    .setFontSize(8).setFontColor(grayColor).setBold());
            clientCell.add(new Paragraph(facture.getClientNom())
                    .setBold().setFontSize(11).setFontColor(textColor).setMarginBottom(4));
            if (facture.getClientAdresse() != null)
                clientCell.add(new Paragraph(facture.getClientAdresse()).setFontSize(9).setFontColor(grayColor));
            if (facture.getClientEmail() != null)
                clientCell.add(new Paragraph(facture.getClientEmail()).setFontSize(9).setFontColor(grayColor));
            if (facture.getClientTelephone() != null)
                clientCell.add(new Paragraph(facture.getClientTelephone()).setFontSize(9).setFontColor(grayColor));
            clientTable.addCell(clientCell);

            // Droite
            Cell refCell = new Cell().setBorder(Border.NO_BORDER);
            refCell.add(new Paragraph("RÉFÉRENCE")
                    .setFontSize(8).setFontColor(grayColor).setBold()
                    .setTextAlignment(TextAlignment.RIGHT));
            String ref = facture.getCommandeAchat() != null ?
                    "Achat " + facture.getCommandeAchat().getNumero() :
                    "Commande " + facture.getCommande().getNumero();
            refCell.add(new Paragraph(ref)
                    .setBold().setFontSize(11).setFontColor(textColor)
                    .setTextAlignment(TextAlignment.RIGHT));
            clientTable.addCell(refCell);

            document.add(clientTable);
            document.add(new Paragraph("\n"));

            // ============================================================
            // TABLEAU DES PRODUITS
            // ============================================================
            Table itemsTable = new Table(UnitValue.createPercentArray(new float[]{4, 2, 1, 2}));
            itemsTable.setWidth(UnitValue.createPercentValue(100));

            // En-têtes
            itemsTable.addHeaderCell(new Cell().add(new Paragraph("Description").setBold().setFontSize(9).setFontColor(ColorConstants.WHITE))
                    .setBackgroundColor(primaryColor).setBorder(Border.NO_BORDER).setPadding(6));
            itemsTable.addHeaderCell(new Cell().add(new Paragraph("Prix Unitaire").setBold().setFontSize(9).setFontColor(ColorConstants.WHITE))
                    .setBackgroundColor(primaryColor).setBorder(Border.NO_BORDER).setPadding(6).setTextAlignment(TextAlignment.RIGHT));
            itemsTable.addHeaderCell(new Cell().add(new Paragraph("Qté").setBold().setFontSize(9).setFontColor(ColorConstants.WHITE))
                    .setBackgroundColor(primaryColor).setBorder(Border.NO_BORDER).setPadding(6).setTextAlignment(TextAlignment.CENTER));
            itemsTable.addHeaderCell(new Cell().add(new Paragraph("Total").setBold().setFontSize(9).setFontColor(ColorConstants.WHITE))
                    .setBackgroundColor(primaryColor).setBorder(Border.NO_BORDER).setPadding(6).setTextAlignment(TextAlignment.RIGHT));

            // Lignes
            if (facture.getCommande() != null) {
                for (LigneCommande ligne : facture.getCommande().getLignes()) {
                    addRow(itemsTable, ligne.getProduit().getNom(), ligne.getPrixUnitaire(), ligne.getQuantite(), ligne.getSousTotal());
                }
            } else if (facture.getCommandeAchat() != null) {
                for (LigneCommandeAchat ligne : facture.getCommandeAchat().getLignes()) {
                    addRow(itemsTable, ligne.getProduit().getNom(), ligne.getPrixAchatUnitaire(), ligne.getQuantite(), ligne.getSousTotal());
                }
            }

            document.add(itemsTable);
            document.add(new Paragraph("\n"));

            // ============================================================
            // TOTAUX
            // ============================================================
            Table totalsTable = new Table(UnitValue.createPercentArray(new float[]{7, 3}));
            totalsTable.setWidth(UnitValue.createPercentValue(100));

            // Total HT
            totalsTable.addCell(new Cell().add(new Paragraph("Total HT").setFontSize(9).setFontColor(grayColor))
                    .setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT));
            totalsTable.addCell(new Cell().add(new Paragraph(formatMoney(facture.getTotalHT())).setFontSize(9).setFontColor(textColor))
                    .setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT));

            // TVA
            totalsTable.addCell(new Cell().add(new Paragraph("TVA (" + facture.getTvaPercent() + "%)").setFontSize(9).setFontColor(grayColor))
                    .setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT));
            totalsTable.addCell(new Cell().add(new Paragraph(formatMoney(facture.getTotalTTC().subtract(facture.getTotalHT()))).setFontSize(9).setFontColor(textColor))
                    .setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT));

            // Total TTC
            totalsTable.addCell(new Cell().add(new Paragraph("Total TTC").setBold().setFontSize(11).setFontColor(textColor))
                    .setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT).setBackgroundColor(new DeviceRgb(243, 244, 246)).setPadding(6));
            totalsTable.addCell(new Cell().add(new Paragraph(formatMoney(facture.getTotalTTC())).setBold().setFontSize(11).setFontColor(primaryColor))
                    .setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT).setBackgroundColor(new DeviceRgb(243, 244, 246)).setPadding(6));

            document.add(totalsTable);

            // ============================================================
            // PIED DE PAGE
            // ============================================================
            document.add(new Paragraph("\n\n"));
            document.add(new Paragraph("Merci de votre confiance !")
                    .setFontSize(9).setFontColor(grayColor).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph(facture.getEntreprise().getNom() + " - Généré par InnovaTech")
                    .setFontSize(8).setFontColor(grayColor).setTextAlignment(TextAlignment.CENTER));

            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    private void addRow(Table table, String nom, BigDecimal prix, Integer qte, BigDecimal total) {
        table.addCell(new Cell().add(new Paragraph(nom).setFontSize(9))
                .setBorderBottom(new com.itextpdf.layout.borders.SolidBorder(ColorConstants.LIGHT_GRAY, 1))
                .setBorderTop(Border.NO_BORDER).setBorderLeft(Border.NO_BORDER).setBorderRight(Border.NO_BORDER)
                .setPadding(6));
        table.addCell(new Cell().add(new Paragraph(formatMoney(prix)).setFontSize(9))
                .setBorderBottom(new com.itextpdf.layout.borders.SolidBorder(ColorConstants.LIGHT_GRAY, 1))
                .setBorderTop(Border.NO_BORDER).setBorderLeft(Border.NO_BORDER).setBorderRight(Border.NO_BORDER)
                .setPadding(6).setTextAlignment(TextAlignment.RIGHT));
        table.addCell(new Cell().add(new Paragraph(qte.toString()).setFontSize(9))
                .setBorderBottom(new com.itextpdf.layout.borders.SolidBorder(ColorConstants.LIGHT_GRAY, 1))
                .setBorderTop(Border.NO_BORDER).setBorderLeft(Border.NO_BORDER).setBorderRight(Border.NO_BORDER)
                .setPadding(6).setTextAlignment(TextAlignment.CENTER));
        table.addCell(new Cell().add(new Paragraph(formatMoney(total)).setFontSize(9).setBold())
                .setBorderBottom(new com.itextpdf.layout.borders.SolidBorder(ColorConstants.LIGHT_GRAY, 1))
                .setBorderTop(Border.NO_BORDER).setBorderLeft(Border.NO_BORDER).setBorderRight(Border.NO_BORDER)
                .setPadding(6).setTextAlignment(TextAlignment.RIGHT));
    }

    private String formatMoney(BigDecimal value) {
        DecimalFormat df = new DecimalFormat("#,##0.00");
        return df.format(value) + " FCFA";
    }
}