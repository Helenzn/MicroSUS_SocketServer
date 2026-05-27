package server;

public class PartidaParImpar {
    private Integer valorJogada1;
    private Boolean jogada1EhPar;
    private Integer valorJogada2;
    private Boolean jogada2EhPar;
    private Integer idJogada;

    public PartidaParImpar(Integer idJogada) {
        this.idJogada = idJogada;
        this.valorJogada1 = null;
        this.jogada1EhPar = null;
        this.valorJogada2 = null;
        this.jogada2EhPar = null;
    }

    public Integer getValorJogada1() {
        return valorJogada1;
    }

    public Boolean getJogada1EhPar() {
        return jogada1EhPar;
    }

    public Integer getValorJogada2() {
        return valorJogada2;
    }

    public Boolean getJogada2EhPar() {
        return jogada2EhPar;
    }

    public Integer getIdJogada() {
        return idJogada;
    }

}
